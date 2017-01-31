from collections import OrderedDict

# event|time	| from_n to_n type size	flags  | fid src_addr dest_addr sq_num pid
# 0    |  1     | 2      3    4    5       6   |  7    8        9         10    11
# +	   |6.4176 	| 1 	 2 	 cbr  1000 ------- |  2   1.0 	 2.0 		7897  8061
# r    |6.41790 | 1	     2 	 cbr  1000 ------- |  2   1.0 	 2.0 		7835  7999
RECEIVE = 'r'
ENQUEUE = '+'
DEQUEUE = '-'
DROP = 'd'
N1 = ['0', '0.0']
N2 = ['1', '1.0']
N3 = ['2', '2.0']
N4 = ['3', '3.0']
N5 = ['4', '4.0']
N6 = ['5', '5.0']
NODES = [N1, N2, N3, N4, N5, N6]
TCP_TYPE = 'tcp'
CBR_TYPE = 'cbr'
ACK_TYPE = 'ack'
# index of one-line data 
DATA_EVENT = 0
DATA_TIME = 1
DATA_FROM_NODE = 2
DATA_TO_NODE = 3
DATA_TYPE = 4
DATA_SIZE = 5
DATA_FID = 7
DATA_SRC_ADDR = 8
DATA_DEST_ARRD = 9
DATA_SEQ_NUM = 10
DATA_PID = 11


def parse_trace_for_ex12(filepath, source, dest):
    tcp_throughput = 0
    tcp_drops = 0
    tcp_pids = set()
    tcp_rtt = dict()

    start_time = 0
    end_time = 0

    src_node = NODES[source]
    dest_node = NODES[dest]
    with open(filepath, 'r') as f:
        # iterate over every line of this file
        for line in f:
            data = line.split()
            if len(data) != 12:
                continue
            # define each part of data
            data_event = data[DATA_EVENT]
            time = float(data[DATA_TIME])
            from_node = data[DATA_FROM_NODE]
            to_node = data[DATA_TO_NODE]
            data_type = data[DATA_TYPE]
            packet_size = int(data[DATA_SIZE])
            fid = data[DATA_FID]
            src_addr = data[DATA_SRC_ADDR]
            dest_addr = data[DATA_DEST_ARRD]
            seq_num = data[DATA_SEQ_NUM]
            pid = data[DATA_PID]
            # skip the CBR packets
            if data_type == CBR_TYPE:
                continue

            # remove every packet's sequence number and the start_time, end_time
            if len(tcp_pids) == 0:
                start_time = time
            tcp_pids.add(pid)
            end_time = time

            # count packet drops
            if data_event == DROP and ((src_node[1] == src_addr and dest_node[1] == dest_addr) or (
                            src_node[1] == dest_addr and dest_node[1] == src_addr)):
                tcp_drops += 1
            # a TCP packet sent from src_node, to dest_node, record the time for RTT
            elif data_event == ENQUEUE and \
                            data_type == TCP_TYPE and \
                            from_node == src_node[0] and \
                            dest_addr == dest_node[1] and \
                            seq_num not in tcp_rtt:
                tcp_rtt[seq_num] = [time, -1, -1]
            elif data_event == RECEIVE:
                # a ACK packet sent from dest_node, to src_node,
                if data_type == ACK_TYPE and \
                                to_node == src_node[0] and \
                                dest_addr == src_node[1] and \
                                seq_num in tcp_rtt:
                    tcp_rtt[seq_num][1] = time
                    tcp_rtt[seq_num][2] = tcp_rtt[seq_num][1] - tcp_rtt[seq_num][0]
                    tcp_throughput += packet_size
                elif data_type == TCP_TYPE and \
                                to_node == dest_node[0] and \
                                src_addr == src_node[1]:
                    tcp_throughput += packet_size
        features = dict()
        # calculate the throughput, in Mbps
        features['throughput'] = float((tcp_throughput * 8) / (2 ** 20)) / (end_time - start_time)
        # calculate the drop rate
        features['drop'] = float(tcp_drops) / len(tcp_pids)
        acked_seq_nums = filter(lambda x: -1 not in tcp_rtt[x], tcp_rtt)
        # calculate the latency (or RTT)
        if len(acked_seq_nums) == 0:
            features['latency'] = 0
        else:
            features['latency'] = sum(
                map(lambda x: float(tcp_rtt[x][2]), acked_seq_nums)) / len(acked_seq_nums)
        return features


# generate the result file, which will be used for the gnuplot of experiment 1/2
def generate_result_file_for_ex12(file_path, statistical_result, tested_variants, duration, times):
    for feature in statistical_result:
        result_file = file_path + feature + "-" + "-".join(tested_variants) + '.out'
        feature_dict = statistical_result[feature]
        with open(result_file, 'w') as f:
            f.write('# Feature: ' + feature + '\n')
            f.write('# Experiment_time: ' + str(duration) + '\n')
            f.write("# rate  " + "  ".join(tested_variants) + "\n")
            for rate in feature_dict:
                line = str(rate) + "MB"
                for tcp_variant in tested_variants:
                    line = line + "  " + str(feature_dict[rate][tcp_variant] / times)
                f.write(line + "\n")


def parse_trace_for_ex3(filepath):
    tcp_throughput = 0
    tcp_rtt = dict()
    src_node = NODES[0]
    dest_node = NODES[3]
    # define time range features dict
    time_range_features = OrderedDict()
    time_range_features[0] = {'throughput': 0, 'latency': 0}
    time_range_upper = 2
    with open(filepath, 'r') as f:
        for line in f:
            data = line.split()
            # skip illegal trace log entry
            if len(data) != 12:
                continue
            # define each part of data
            data_event = data[DATA_EVENT]
            time = float(data[DATA_TIME])
            from_node = data[DATA_FROM_NODE]
            to_node = data[DATA_TO_NODE]
            data_type = data[DATA_TYPE]
            packet_size = int(data[DATA_SIZE])
            src_addr = data[DATA_SRC_ADDR]
            dest_addr = data[DATA_DEST_ARRD]
            seq_num = data[DATA_SEQ_NUM]
            # skip the CBR packets
            if data_type == CBR_TYPE:
                continue

            # if this line reaches the upper border of a time range, calculate the average throughput and latency
            # , and reset the throughput, rtt, then add the time range upper border
            if time >= time_range_upper:
                ranged_throughput = float(tcp_throughput * 8) / (2 ** 20)
                acked_seq_nums = filter(lambda x: -1 not in tcp_rtt[x], tcp_rtt)
                if len(acked_seq_nums) != 0:
                    ranged_latency = sum(
                        map(lambda x: float(tcp_rtt[x][2]), acked_seq_nums)) / len(acked_seq_nums)
                time_range_features[time_range_upper] = {'throughput': ranged_throughput, 'latency': ranged_latency}
                time_range_upper += 2
                tcp_throughput = 0
                tcp_rtt = {k: v for k, v in tcp_rtt.items() if k == -1}

            # a TCP packet sent from src_node, to dest_node, record the time for RTT
            if data_event == ENQUEUE and \
                            data_type == TCP_TYPE and \
                            from_node == src_node[0] and \
                            dest_addr == dest_node[1] and \
                            seq_num not in tcp_rtt:
                tcp_rtt[seq_num] = [time, -1, -1]
            elif data_event == RECEIVE:
                # a ACK packet sent from dest_node, to src_node,
                if data_type == ACK_TYPE and \
                                to_node == src_node[0] and \
                                dest_addr == src_node[1] and \
                                seq_num in tcp_rtt:
                    tcp_rtt[seq_num][1] = time
                    tcp_rtt[seq_num][2] = tcp_rtt[seq_num][1] - tcp_rtt[seq_num][0]
                    tcp_throughput += packet_size
                elif data_type == TCP_TYPE and \
                                to_node == dest_node[0] and \
                                src_addr == src_node[1]:
                    tcp_throughput += packet_size
    return time_range_features


# generate the result file, which will be used for the gnuplot of experiment 1/2
def generate_result_file_for_ex3(file_path, statistical_result, tested_queue_types, duration):
    for feature in statistical_result:
        result_file = file_path + feature + '.out'
        feature_dict = statistical_result[feature]
        with open(result_file, 'w') as f:
            f.write('# Feature: ' + feature + '\n')
            f.write('# Experiment_time: ' + str(duration) + '\n')

            i = 0
            f.write("# rate  " + "  ".join(tested_queue_types) + "\n")
            for time_second in feature_dict:
                if i == 0:
                    f.write("# time  " + "  ".join(feature_dict[time_second]) + "\n")
                i += 1
                line = str(time_second) + "s"
                for comb_key in feature_dict[time_second]:
                    line = line + "  " + str(feature_dict[time_second][comb_key])
                f.write(line + "\n")