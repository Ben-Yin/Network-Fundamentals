package dns;

/**
 * Created by HappyMole on 11/29/16.
 */
public class MeasureResult implements Comparable {

    private float latency;

    private String ip;

    public MeasureResult(String ip, float latency) {
        this.ip = ip;
        this.latency = latency;
    }

    public float getLatency() {
        return latency;
    }

    public void setLatency(float latency) {
        this.latency = latency;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    @Override
    public int compareTo(Object o) {
        MeasureResult measureResult = (MeasureResult)o;

        if (latency == measureResult.latency) {
            return 0;
        } else if (latency < measureResult.latency) {
            return -1;
        } else {
            return 1;
        }
    }
}
