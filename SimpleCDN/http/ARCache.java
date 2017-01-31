package http;

import utils.CDNLogger;
import utils.CacheUtils;

import java.io.*;
import java.util.HashMap;

/**
 * Created by BenYin on 11/25/16.
 */
// Adaptive Replacement Cache
public class ARCache {
    private static final String CACHE_ROOT_FOLDER = "temp/";

    public enum KEY_STATUS {
        KEY_UNFOUNDED, KEY_IN_LRU, KEY_IN_LFU, KEY_IN_GHOST_LRU, KEY_IN_GHOST_LFU}

    private int capacity = 10 * 1024 * 1024;

    private Node LRUHead = new Node(null);
    private Node LRUTail = new Node(null);
    private Node LFUHead = new Node(null);
    private Node LFUTail = new Node(null);

    private Node ghostLRUHead = new Node(null);
    private Node ghostLRUTail = new Node(null);
    private Node ghostLFUHead = new Node(null);
    private Node ghostLFUTail = new Node(null);

    private int LRUCapacity = capacity / 2;
    private int LFUCapacity = capacity / 2;
    private int ghostLRUSize = 100;
    private int ghostLFUSize = 100;
    private int currentLRUSize = 0;
    private int currentLFUSize = 0;
    private int currentGhostLRUSize = 0;
    private int currentGhostLFUSize = 0;

    private HashMap<String, Node> LRUMap = new HashMap<String, Node>();
    private HashMap<String, Node> LFUMap = new HashMap<String, Node>();
    private HashMap<String, Node> ghostLRUMap = new HashMap<String, Node>();
    private HashMap<String, Node> ghostLFUMap = new HashMap<String, Node>();


    public ARCache() {
        File file = new File(CACHE_ROOT_FOLDER);
        if (!file.exists()) {
            file.mkdir();
        }

        LRUHead.next = LRUTail;
        LRUTail.prev = LRUHead;
        LFUHead.next = LFUTail;
        LFUTail.prev = LFUHead;
        ghostLRUHead.next = ghostLRUTail;
        ghostLRUTail.prev = ghostLRUHead;
        ghostLFUHead.next = ghostLFUTail;
        ghostLFUTail.prev = ghostLFUHead;
    }

    public KEY_STATUS exists(String filePath) {
        filePath = CACHE_ROOT_FOLDER + filePath;

        // request content in LRU cache
        if (LRUMap.containsKey(filePath)) {
            CDNLogger.info("file is in LRUMap: " + filePath);
            return KEY_STATUS.KEY_IN_LRU;
            // request content in LFU cache
        } else if (LFUMap.containsKey(filePath)) {
            CDNLogger.info("file is in LFUMap: " + filePath);
            return KEY_STATUS.KEY_IN_LFU;
            // request content in ghostLRUMap cache
        } else if (ghostLRUMap.containsKey(filePath)) {
            CDNLogger.info("file is in ghostLRUMap: " + filePath);
            return KEY_STATUS.KEY_IN_GHOST_LRU;
            // request content in ghostLRUMap cache
        } else if (ghostLFUMap.containsKey(filePath)) {
            CDNLogger.info("file is in ghostLFUMap: " + filePath);
            return KEY_STATUS.KEY_IN_GHOST_LFU;
            // request content not in any cache
        } else {
            return KEY_STATUS.KEY_UNFOUNDED;
        }
    }

    public byte[] readCache(String filePath, KEY_STATUS status) {
        filePath = CACHE_ROOT_FOLDER + filePath;

        byte[] content = null;
        // request content in LRU cache
        if (status == KEY_STATUS.KEY_IN_LRU) {
            Node node = LRUMap.get(filePath);
            content = CacheUtils.readFromFile(filePath);
            // if content in LRU cache, means content match twice, move from LRU to LFU cache
            LRUToLFU(node, content.length);
            // request content in LFU cache
        } else if (status == KEY_STATUS.KEY_IN_LFU) {
            Node node = LFUMap.get(filePath);
            node.deleteLink();
            moveToHead(node, LFUHead);
            content = CacheUtils.readFromFile(filePath);
        }
        return content;
    }

    public void writeCache(String filePath, byte[] data, KEY_STATUS status) {
        filePath = CACHE_ROOT_FOLDER + filePath;

        // if key match in ghostLRU, increase LRU cache size, decrease LFU cache size
        // write content in the LRU cache
        if (status == KEY_STATUS.KEY_IN_GHOST_LRU) {
            adjustLFUSize(data.length);
            Node node = ghostLRUMap.get(filePath);
            ghostLRUMap.remove(filePath);
            node.deleteLink();
            currentGhostLRUSize -= 1;
            moveToHead(node, LRUHead);
            LRUCapacity += data.length;
            CacheUtils.writeToFile(filePath, data);
            CDNLogger.info("write cache to LRU, capacity is: " + LRUCapacity);
            // if key match in ghostLFU, increase LFU cache size, decrease LRU cache size
            // write content in the LFU cache
        } else if (status == KEY_STATUS.KEY_IN_GHOST_LFU) {
            adjustLRUSize(data.length);
            Node node = ghostLFUMap.get(filePath);
            ghostLFUMap.remove(filePath);
            node.prev.next = node.next;
            node.next.prev = node.prev;
            currentGhostLFUSize -= 1;
            moveToHead(node, LFUHead);
            LFUCapacity += data.length;
            CacheUtils.writeToFile(filePath, data);
            CDNLogger.info("write cache to LFU, capacity is: " + LFUCapacity);
        /* move node from LRU to LFU
         * if key match twice, add key to the head of LFU
         * remove key in LRU
         * key does not match any cache, write content in LRU cache
         */

        } else if (status == KEY_STATUS.KEY_UNFOUNDED) {
            Node node = new Node(filePath);
            moveToHead(node, LRUHead);
            LRUMap.put(node.filePath, node);
            while (currentLRUSize + data.length >= LRUCapacity) {
                CDNLogger.info("LRU is full, clear file, current capacity is: " + LRUCapacity);
                // Least Recently Used Node
                Node LRUNode = LRUToGhost();
                // delete the file in the LRU cache to free more memory
                int clearSize = CacheUtils.clearFileSize(LRUNode.filePath);
                currentLRUSize -= clearSize;
            }

            currentLRUSize += data.length;
            CacheUtils.writeToFile(filePath, data);
        }
    }

    private void LRUToLFU(Node node, int fileSize) {
        node.deleteLink();
        moveToHead(node, LFUHead);
        LFUMap.put(node.filePath, node);
        LRUMap.remove(node.filePath);
        currentLRUSize -= fileSize;
        while (currentLFUSize + fileSize >= LFUCapacity) {
            // Least Frequently Used Node
            Node LFUNode = LFUToGhost();
            // delete the file in the LFU cache to free more memory
            int clearSize = CacheUtils.clearFileSize(LFUNode.filePath);
            currentLFUSize -= clearSize;
        }
        currentLFUSize += fileSize;
    }

    private Node LFUToGhost() {
        /* move node from LFU to ghostLFU
         * if LFU is full, add key to the head of ghostLFU
         * remove key in LFU
         */
        Node node = removeFromTail(LFUTail);
        ghostLFUMap.put(node.filePath, node);
        LFUMap.remove(node.filePath);
        moveToHead(node, ghostLFUHead);
        if (currentGhostLFUSize == ghostLFUSize) {
            Node ghostLFUNode = removeFromTail(ghostLFUTail);
            ghostLFUMap.remove(ghostLFUNode.filePath);
        } else {
            currentGhostLFUSize++;
        }
        return node;
    }

    private Node LRUToGhost() {
        /* move node from LRU to ghostLRU
         * if LRU is full, add key to the head of ghostLRU
         * remove key in LRU
         */
        Node node = removeFromTail(LRUTail);
        ghostLRUMap.put(node.filePath, node);
        LRUMap.remove(node.filePath);
        moveToHead(node, ghostLRUHead);
        if (currentGhostLRUSize == ghostLRUSize) {
            Node ghostLRUNode = removeFromTail(ghostLRUTail);
            ghostLRUMap.remove(ghostLRUNode.filePath);
        } else {
            currentGhostLRUSize++;
        }
        return node;
    }

    private void adjustLFUSize(int fileSize) {
        if (LFUCapacity < fileSize) {
            return;
        }
        while (currentLFUSize >= LFUCapacity - fileSize) {
            // Least Frequently Used Node
            Node LFUNode = LFUToGhost();
            int spareSize = CacheUtils.clearFileSize(LFUNode.filePath);
            currentLFUSize -= spareSize;
        }
        LFUCapacity -= fileSize;
    }

    private void adjustLRUSize(int fileSize) {
        if (LRUCapacity < fileSize) {
            return;
        }
        while (currentLRUSize >= LRUCapacity - fileSize) {
            // Least Recently Used Node
            Node LRUNode = LRUToGhost();
            int spareSize = CacheUtils.clearFileSize(LRUNode.filePath);
            currentLRUSize -= spareSize;
        }
        LRUCapacity -= fileSize;
    }

    private void moveToHead(Node node, Node head) {
        node.next = head.next;
        head.next.prev = node;
        node.prev = head;
        head.next = node;
    }

    private Node removeFromTail(Node tail) {
        Node node = tail.prev;
        node.prev.next = tail;
        tail.prev = node.prev;
        return node;
    }


    private class Node {
        Node prev;
        Node next;
        String filePath;

        public Node(String filePath) {
            this.filePath = filePath;
            this.prev = null;
            this.next = null;
        }

        public void deleteLink() {
            this.next.prev = this.prev;
            this.prev.next = this.next;
        }
    }
}
