package http;

import utils.CDNLogger;
import utils.CacheUtils;

import java.util.Hashtable;
import java.util.Map;

/**
 * Created by BenYin on 11/24/16.
 */
public class LRUCache {

    private static final String CACHE_ROOT_FOLDER = "temp/";

    private int capacity = 10 * 1024 * 1024;
    private int currentCacheSize = 0;
    // HashMap<filePath, Node>
    private Map<String, Node> cacheMap = new Hashtable<String, Node>();
    private Node head = new Node(null);
    private Node tail = new Node(null);

    public LRUCache() {
        head.next = tail;
        tail.next = head;
    }

    public byte[] readCache(String filePath) {
        filePath = CACHE_ROOT_FOLDER + filePath;
        synchronized (cacheMap) {
            if (cacheMap.containsKey(filePath)) {
                CDNLogger.info("request hit LRU cache:" + filePath);
                Node node = cacheMap.get(filePath);
                byte[] content = CacheUtils.readFromFile(node.filePath);
                node.next.prev = node.prev;
                node.prev.next = node.next;
                moveToHead(node);
                return content;
            } else {
                CDNLogger.info("request not in LRU cache:" + filePath);
                return null;
            }
        }
    }

    public void writeCache(String filePath, byte[] data) {
        filePath = CACHE_ROOT_FOLDER + filePath;
        Node node = new Node(filePath);
        synchronized (cacheMap) {
            cacheMap.put(filePath, node);
            moveToHead(node);
            CDNLogger.info("LRU cache size:" + String.valueOf(currentCacheSize / 1024) + "KB");
            while (currentCacheSize + data.length >= capacity) {
                Node LRUNode = removeFromTail(tail);
                cacheMap.remove(LRUNode.filePath);
                int clearSize = CacheUtils.clearFileSize(LRUNode.filePath);
                currentCacheSize -= clearSize;
            }
        }
        currentCacheSize += data.length;
        CacheUtils.writeToFile(filePath, data);
    }

    public void moveToHead(Node node) {
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
    }
}