package dns;

import utils.ArrayUtils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Created by HappyMole on 11/22/16.
 */
public class DnsPacket {

    // Reference: http://www.ietf.org/rfc/rfc1035.txt

    // A 16 bit identifier assigned by the program that generates any kind of query
    private static final int ID_OFFSET = 0;

    // flags
    private static final int FLAGS_OFFSET = 2;

    // an unsigned 16 bit integer specifying the number of entries in the question section
    private static final int QD_COUNT_OFFSET = 4;

    // an unsigned 16 bit integer specifying the number of resource records in the answer section.
    private static final int AN_COUNT_OFFSET = 6;

    // an unsigned 16 bit integer specifying the number of name server resource records in the authority records section.
    private static final int NS_COUNT_OFFSET = 8;

    // an unsigned 16 bit integer specifying the number of resource records in the additional records section.
    private static final int AR_COUNT_OFFSET = 10;

    // after index 12 is all content of the DNS packet
    private static final int CONTENT_OFFSET = 12;

    private byte[] _data_;

    public DnsPacket(byte[] data) {
        this._data_ = data;
    }

    public int getMessageType() {
        return _data_[FLAGS_OFFSET] >> 7;
    }

    public String getDomainName() {
        StringBuilder domainNameBuilder = new StringBuilder();
        int i = 12;
        while (i + 1 < _data_.length) {
            if (((_data_[i] & 0xff) | (_data_[i + 1] & 0xff)) == 0) {
                break;
            } else {
                if (domainNameBuilder.length() != 0) {
                    domainNameBuilder.append(".");
                }
                int len = _data_[i];
                i++;
                domainNameBuilder.append(new String(_data_, i, len));
                i += len;
            }
        }
        return domainNameBuilder.toString();
    }

    public byte[] buildResponse(String ip) {
        // set to response flag
        _data_[FLAGS_OFFSET] |= (byte) 0x80;
        // server can do recursive queries
        _data_[FLAGS_OFFSET + 1] = (byte) 0x80;
        // set to one answer
        _data_[AN_COUNT_OFFSET] &= 0x00;
        _data_[AN_COUNT_OFFSET + 1] |= 0x01;

        byte[] answer = createAnswer(ip);

        byte[] response = ArrayUtils.concatArrays(_data_, answer);

        return response;
    }

    public byte[] createAnswer(String ip) {
        ByteArrayOutputStream data = new ByteArrayOutputStream();
        DataOutputStream stream = new DataOutputStream(data);
        try {
            // write domain name
            stream.writeShort(0xc00c);
            // write type: A
            stream.writeShort(0x0001);
            // write class: IN
            stream.writeShort(0x0001);
            // write ttl: 256
            stream.writeInt(0x000000ff);
            // write RDATA length
            stream.writeShort(0x0004);
            // write ip
            for (String seg : ip.split("\\.")) {
                stream.writeByte((byte) Short.parseShort(seg));
            }
        } catch (IOException e) {
            System.err.println("error happens when write answer data into stream");
        }

        return data.toByteArray();
    }
}