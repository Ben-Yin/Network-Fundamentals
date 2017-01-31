#Create a simulator object
set ns [new Simulator]

#Get arguments from command line
#Queue algorithm
set queue_algorithm [lindex $argv 0]
#TCP variants
set tcp_variant_agent [lindex $argv 1]
set tcp_variant_sink [lindex $argv 2]
set tcp_start [lindex $argv 3]
#CBR flow rate
set cbr_rate [lindex $argv 4]Mb
set cbr_start [lindex $argv 5]
#Finish time
set finish_time [lindex $argv 6]
#output path
set output_path [lindex $argv 7]

#Open the trace file (before you start the experiment!)
set tf [open $output_path w]
$ns trace-all $tf

#Define a 'finish' procedure
proc finish {} {
    global ns tf tcp finish_time
    puts [format "average throughput:%.1f Kbps" \ [expr [$tcp set ack_]* ([$tcp set packetSize_])*8/1024.0/$finish_time]]
    $ns flush-trace
    # Close the trace file (after you finish the experiment!)
    close $tf
    exit 0
}

#Create six nodes
set n1 [$ns node]
set n2 [$ns node]
set n3 [$ns node]
set n4 [$ns node]
set n5 [$ns node]
set n6 [$ns node]

#Create links between the nodes
$ns duplex-link $n1 $n2 10Mb 10ms $queue_algorithm
$ns duplex-link $n5 $n2 10Mb 10ms $queue_algorithm
$ns duplex-link $n2 $n3 10Mb 10ms $queue_algorithm
$ns duplex-link $n3 $n4 10Mb 10ms $queue_algorithm
$ns duplex-link $n3 $n6 10Mb 10ms $queue_algorithm

#Set Queue Size of link (n2-n3) to 10
$ns queue-limit $n2 $n3 10

#Setup a TCP connection
# n1 send TCP -> n4
set tcp [new Agent/$tcp_variant_agent]
$ns attach-agent $n1 $tcp
# n4 'sink' send ACK packets -> n1
set sink [new Agent/$tcp_variant_sink]
$ns attach-agent $n4 $sink
$ns connect $tcp $sink
$tcp set fid_ 1

#Setup a FTP over TCP connection
set ftp [new Application/FTP]
$ftp attach-agent $tcp
$ftp set type_ FTP


#Setup a UDP connection
# CBR: n5 -> n6
set udp [new Agent/UDP]
$ns attach-agent $n5 $udp
set null [new Agent/Null]
$ns attach-agent $n6 $null
$ns connect $udp $null
$udp set fid_ 2

#Setup a CBR over UDP connection
set cbr [new Application/Traffic/CBR]
$cbr attach-agent $udp
$cbr set type_ CBR
$cbr set packet_size_ 1000
$cbr set rate_ $cbr_rate
$cbr set random_ false


#Schedule events for the CBR and FTP agents
$ns at $tcp_start "$ftp start"
$ns at $cbr_start "$cbr start"
$ns at $finish_time "$ftp stop"
$ns at $finish_time "$cbr stop"

#Call the finish procedure after $finish_time seconds of simulation time
$ns at $finish_time "finish"

#Print CBR packet size and interval
puts "Queue algorithm=$queue_algorithm, TCP variant agent=$tcp_variant_agent, TCP variant sink=$tcp_variant_sink, CBR rate=$cbr_rate"

#Run the simulation
$ns run