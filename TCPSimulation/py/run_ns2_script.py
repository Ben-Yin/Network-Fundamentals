#!/usr/bin/env python
import subprocess
import os
import argparse
from parse_trace import parse_trace_for_ex12, generate_result_file_for_ex12, parse_trace_for_ex3, \
    generate_result_file_for_ex3
from collections import OrderedDict

tcp_variants_agent = dict()
tcp_variants_agent['Tahoe'] = 'TCP'
tcp_variants_agent['Reno'] = 'TCP/Reno'
tcp_variants_agent['NewReno'] = 'TCP/Newreno'
tcp_variants_agent['Vegas'] = 'TCP/Vegas'
tcp_variants_agent['SACK'] = 'TCP/Sack1'

# ns_ccis_path = '/course/cs4700f12/ns-allinone-2.35/bin/ns'
ns_ccis_path = '/usr/local/ns-allinone-2.35/ns-2.35/ns'
ex1_path = '../tcl/experiment_1.tcl'
ex2_path = '../tcl/experiment_2.tcl'
ex3_path = '../tcl/experiment_3.tcl'


def parse_commandline_arg():
    parser = argparse.ArgumentParser(description='run NS-2 experiments according to given arguments.')
    # divide into 3 experiments
    subparsers = parser.add_subparsers()
    parse_ex1_arg(subparsers.add_parser('ex1'))
    parse_ex2_arg(subparsers.add_parser('ex2'))
    parse_ex3_arg(subparsers.add_parser('ex3'))
    return parser.parse_args()


# parse arguments for experiment1
def parse_ex1_arg(parser):
    parser.add_argument('-o', '--out', type=str, default='../out/ex1/',
                        help='The output path of experiment1')
    parser.add_argument('-cst', '--cbr_start', type=float, default=0.0,
                        help='The start time for cbr')
    parser.add_argument('-tst', '--tcp_start', type=float, default=0.0,
                        help='The start time for tcp')
    parser.add_argument('-d', '--duration', type=float, default=20.0,
                        help='The running time of experiment1')
    parser.add_argument('-t', '--times', type=int, default=1,
                        help='The experiment times of running NS2')
    parser.set_defaults(which='ex1')


# parse arguments for experiment2
def parse_ex2_arg(parser):
    parser.add_argument('-tst1', '--tcp1_start', type=float, default=0.0,
                        help='The start time for tcp1')
    parser.add_argument('-tsp1', '--tcp1_stop', type=float, default=30.0,
                        help='The stop time for tcp1')
    parser.add_argument('-tst2', '--tcp2_start', type=float, default=0.0,
                        help='The start time for tcp2')
    parser.add_argument('-tsp2', '--tcp2_stop', type=float, default=30.0,
                        help='The stop time for tcp2')
    parser.add_argument('-cst', '--cbr_start', type=float, default=3.0,
                        help='The start time for cbr')
    parser.add_argument('-csp', '--cbr_stop', type=float, default=30.0,
                        help='The stop time for cbr')
    parser.add_argument('-d', '--duration', type=float, default=30.0,
                        help='The running time for this experiment')
    parser.add_argument('-o', '--out', type=str, default='../out/ex2/',
                        help='The output path of experiment2')
    parser.add_argument('-t', '--times', type=int, default=1,
                        help='The experiment times of running NS2')
    parser.set_defaults(which='ex2')


# parse arguments for experiment3
def parse_ex3_arg(parser):
    parser.add_argument('-tst', '--tcp_start', type=float, default=0.0,
                        help='The start time for tcp')
    parser.add_argument('-c', '--cbr', type=float, default=7.5,
                        help='The cbr for this experiment')
    parser.add_argument('-cst', '--cbr_start', type=float, default=3.0,
                        help='The start time for cbr')
    parser.add_argument('-d', '--duration', type=float, default=31.0,
                        help='The running time for this experiment')
    parser.add_argument('-o', '--out', type=str, default='../out/ex3/',
                        help='The output path of experiment3')
    parser.set_defaults(which='ex3')


# run experiment1 with given arguments
def run_ns2_ex1(arguments):
    # create all initial variables
    test_tcp_variants = ['Tahoe', 'Reno', 'NewReno', 'Vegas']
    trace_out_path = arguments.out
    tcp_start = arguments.tcp_start
    cbr_start = arguments.cbr_start
    duration = arguments.duration
    times = arguments.times

    # create a dict to store all data parsed from the trace log
    statistical_result = dict()
    throughput_dict = OrderedDict()
    drop_rate_dict = OrderedDict()
    latency_dict = OrderedDict()
    statistical_result["throughput"] = throughput_dict
    statistical_result["drop-rate"] = drop_rate_dict
    statistical_result["latency"] = latency_dict

    # run n times of the same experiment to get the statistical significant result
    for n in range(times):
        for tcp_variant in test_tcp_variants:
            for i in range(1, 101):
                rate = float(i) / 10
                if rate not in throughput_dict:
                    throughput_dict[rate] = dict()
                    drop_rate_dict[rate] = dict()
                    latency_dict[rate] = dict()

                trace_file = trace_out_path + tcp_variant + '_cbr_' + str(rate) + '.tr'
                ns_args = [tcp_variants_agent[tcp_variant], tcp_start, rate, cbr_start, duration, trace_file]
                # run ns2 with the given parameters
                run_ns2(ex1_path, ns_args)
                features = parse_trace_for_ex12(trace_file, 0, 3)
                # remove the trace file after parsing
                os.remove(trace_file)
                if tcp_variant not in throughput_dict[rate]:
                    throughput_dict[rate][tcp_variant] = 0
                    drop_rate_dict[rate][tcp_variant] = 0
                    latency_dict[rate][tcp_variant] = 0
                throughput_dict[rate][tcp_variant] = throughput_dict[rate][tcp_variant] + features['throughput']
                drop_rate_dict[rate][tcp_variant] = drop_rate_dict[rate][tcp_variant] + features['drop']
                latency_dict[rate][tcp_variant] = latency_dict[rate][tcp_variant] + features['latency']
                # features is a map with 'throughput', 'drop', 'latency'

    # generate the final result file, which will be used by plot
    generate_result_file_for_ex12(trace_out_path, statistical_result, test_tcp_variants, duration, times)

    # generate the curve image files
    subprocess.Popen("sh ../plot/plot_ex1.sh throughput", shell=True)
    subprocess.Popen("sh ../plot/plot_ex1.sh drop-rate", shell=True)
    subprocess.Popen("sh ../plot/plot_ex1.sh latency", shell=True)


# run experiment2 with the given arguments
def run_ns2_ex2(arguments):
    # create all initial variables
    trace_out_path = arguments.out
    duration = arguments.duration
    cbr_start_time = arguments.cbr_start
    cbr_stop_time = arguments.cbr_stop
    tcp1_start_time = arguments.tcp1_start
    tcp1_stop_time = arguments.tcp1_stop
    tcp2_start_time = arguments.tcp2_start
    tcp2_stop_time = arguments.tcp2_stop
    times = arguments.times
    variants_pair_list = [['Reno', 'Reno'], ['NewReno', 'Reno'], ['Vegas', 'Vegas'], ['NewReno', 'Vegas']]
    statistical_result = dict()
    throughput_dict = OrderedDict()
    drop_rate_dict = OrderedDict()
    latency_dict = OrderedDict()
    statistical_result["throughput"] = throughput_dict
    statistical_result["drop-rate"] = drop_rate_dict
    statistical_result["latency"] = latency_dict

    # iterate over all variants pair
    for variants_pair in variants_pair_list:
        tcp_variant1 = variants_pair[0]
        tcp_variant2 = variants_pair[1]
        tcp_variant1_agent = tcp_variants_agent[tcp_variant1]
        tcp_variant2_agent = tcp_variants_agent[tcp_variant2]

        # if the two tcp variants have the same name, rename them to avoid confliction
        if tcp_variant1 == tcp_variant2:
            tcp_variant1 += "1"
            tcp_variant2 += "2"

        # repeat the experiment for several times to eliminate the errors for each time
        for n in range(times):
            for i in range(1, 101):
                rate = float(i) / 10
                if rate not in throughput_dict:
                    throughput_dict[rate] = dict()
                    drop_rate_dict[rate] = dict()
                    latency_dict[rate] = dict()

                # init all variables for running the tcl script and run it
                trace_file = trace_out_path + tcp_variant1 + '_' + tcp_variant2 + '_cbr_' + str(rate) + '.tr'
                ns_args = [tcp_variant1_agent, tcp1_start_time, tcp1_stop_time, tcp_variant2_agent,
                           tcp2_start_time, tcp2_stop_time,
                           rate, cbr_start_time, cbr_stop_time, duration, trace_file]
                run_ns2(ex2_path, ns_args)

                # get all features for tcp variant 1 and tcp variant 2
                features1 = parse_trace_for_ex12(trace_file, 0, 3)
                features2 = parse_trace_for_ex12(trace_file, 4, 5)
                # remove the trace file after parsing
                os.remove(trace_file)

                # collect all feature data for the two tcp variants
                if tcp_variant1 not in throughput_dict[rate]:
                    throughput_dict[rate][tcp_variant1] = 0
                    drop_rate_dict[rate][tcp_variant1] = 0
                    latency_dict[rate][tcp_variant1] = 0
                if tcp_variant2 not in throughput_dict[rate]:
                    throughput_dict[rate][tcp_variant2] = 0
                    drop_rate_dict[rate][tcp_variant2] = 0
                    latency_dict[rate][tcp_variant2] = 0

                throughput_dict[rate][tcp_variant1] = throughput_dict[rate][tcp_variant1] + features1['throughput']
                drop_rate_dict[rate][tcp_variant1] = drop_rate_dict[rate][tcp_variant1] + features1['drop']
                latency_dict[rate][tcp_variant1] = latency_dict[rate][tcp_variant1] + features1['latency']
                throughput_dict[rate][tcp_variant2] = throughput_dict[rate][tcp_variant2] + features2['throughput']
                drop_rate_dict[rate][tcp_variant2] = drop_rate_dict[rate][tcp_variant2] + features2['drop']
                latency_dict[rate][tcp_variant2] = latency_dict[rate][tcp_variant2] + features2['latency']
        # generate the final result file for the tcp pair, which will be used by plot
        generate_result_file_for_ex12(trace_out_path, statistical_result, [tcp_variant1, tcp_variant2], duration, times)
        throughput_dict.clear()
        drop_rate_dict.clear()
        latency_dict.clear()
    # generate the curve image files
    subprocess.Popen("sh ../plot/plot_ex2.sh throughput NewReno Reno", shell=True)
    subprocess.Popen("sh ../plot/plot_ex2.sh throughput NewReno Vegas", shell=True)
    subprocess.Popen("sh ../plot/plot_ex2.sh throughput Reno1 Reno2", shell=True)
    subprocess.Popen("sh ../plot/plot_ex2.sh throughput Vegas1 Vegas2", shell=True)
    subprocess.Popen("sh ../plot/plot_ex2.sh latency NewReno Reno", shell=True)
    subprocess.Popen("sh ../plot/plot_ex2.sh latency NewReno Vegas", shell=True)
    subprocess.Popen("sh ../plot/plot_ex2.sh latency Reno1 Reno2", shell=True)
    subprocess.Popen("sh ../plot/plot_ex2.sh latency Vegas1 Vegas2", shell=True)
    subprocess.Popen("sh ../plot/plot_ex2.sh drop-rate NewReno Reno", shell=True)
    subprocess.Popen("sh ../plot/plot_ex2.sh drop-rate NewReno Vegas", shell=True)
    subprocess.Popen("sh ../plot/plot_ex2.sh drop-rate Reno1 Reno2", shell=True)
    subprocess.Popen("sh ../plot/plot_ex2.sh drop-rate Vegas1 Vegas2", shell=True)


# run experiment3 with given arguments
def run_ns2_ex3(arguments):
    # create all initial variables
    trace_out_path = arguments.out
    duration = arguments.duration
    tcp_start = arguments.tcp_start
    cbr = arguments.cbr
    cbr_start = arguments.cbr_start

    statistical_result = OrderedDict()
    throughput_dict = OrderedDict()
    latency_dict = OrderedDict()
    statistical_result["throughput"] = throughput_dict
    statistical_result["latency"] = latency_dict

    tcp_variants_sink = OrderedDict()
    tcp_variants_sink['Reno'] = 'TCPSink'
    tcp_variants_sink['SACK'] = 'TCPSink/Sack1'

    # define the constant list of queue type and tcp variants, which will be combined
    test_queue_types = ['DropTail', 'RED']
    test_tcp_variants = ['Reno', 'SACK']
    # run experiment for every combination
    for queue_algorithm in test_queue_types:
        for tcp_variant in test_tcp_variants:
            comb_key = tcp_variant + '-' + queue_algorithm
            tcp_agent = tcp_variants_agent[tcp_variant]
            tcp_sink = tcp_variants_sink[tcp_variant]
            trace_file = trace_out_path + tcp_variant + '_' + queue_algorithm + '.tr'
            ns_args = [queue_algorithm, tcp_agent, tcp_sink, tcp_start, cbr, cbr_start, duration, trace_file]
            run_ns2(ex3_path, ns_args)
            ranged_feature_dict = parse_trace_for_ex3(trace_file)
            # remove the trace file after parsing it
            os.remove(trace_file)
            # get the parsed result for every time range, and collect them into an overall dict
            for time_range_upper in ranged_feature_dict:
                if time_range_upper not in throughput_dict:
                    throughput_dict[time_range_upper] = OrderedDict()
                    latency_dict[time_range_upper] = OrderedDict()
                throughput_dict[time_range_upper][comb_key] = ranged_feature_dict[time_range_upper]['throughput']
                latency_dict[time_range_upper][comb_key] = ranged_feature_dict[time_range_upper]['latency']
    # generate the result file for experiment 3
    generate_result_file_for_ex3(trace_out_path, statistical_result, test_queue_types, duration)
    # generate the curve image files
    subprocess.Popen("sh ../plot/plot_ex3.sh throughput", shell=True)
    subprocess.Popen("sh ../plot/plot_ex3.sh latency", shell=True)


def run_ns2(script_path, ns_args):
    cmd = [ns_ccis_path, script_path] + ns_args
    cmd = (map(str, cmd))
    try:
        return subprocess.call(cmd)
    except Exception as e:
        print 'run ns2 failed!'
        raise
    finally:
        pass


if __name__ == "__main__":
    args = parse_commandline_arg()
    if args.which == 'ex1':
        run_ns2_ex1(args)
    elif args.which == 'ex2':
        run_ns2_ex2(args)
    elif args.which == 'ex3':
        run_ns2_ex3(args)
