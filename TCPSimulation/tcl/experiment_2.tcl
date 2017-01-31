#Create a simulator object
set ns [new Simulator]

#Get arguments from command line
#TCP variants
set tcp_variant1 [lindex $argv 0]
set tcp1_start [lindex $argv 1]
set tcp1_stop [lindex $argv 2]
set tcp_variant2 [lindex $argv 3]
set tcp2_start [lindex $argv 4]
set tcp2_stop [lindex $argv 5]

#CBR flow rate
set cbr_rate [lindex $argv 6]Mb
set cbr_start [lindex $argv 7]
set cbr_stop [lindex $argv 8]

# duration
set duration [lindex $argv 9]

# output path
set output_path [lindex $argv 10]


#Open the trace file (before you start the experiment!)
set tf [open $output_path w]
$ns trace-all $tf

#Define a 'finish' procedure
proc finish {} {
        global ns tf tcp1 tcp2 duration
        # print the throughput for the two tcp variants in this process
        puts [format "average throughput:%.1f Kbps" \ [expr [$tcp1 set ack_]* ([$tcp1 set packetSize_])*8/1024.0/$duration]]
        puts [format "average throughput:%.1f Kbps" \ [expr [$tcp2 set ack_]* ([$tcp2 set packetSize_])*8/1024.0/$duration]]
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
$ns duplex-link $n1 $n2 10Mb 10ms DropTail
$ns duplex-link $n5 $n2 10Mb 10ms DropTail
$ns duplex-link $n2 $n3 10Mb 10ms DropTail
$ns duplex-link $n3 $n4 10Mb 10ms DropTail
$ns duplex-link $n3 $n6 10Mb 10ms DropTail

#Give node position (for NAM)
#$ns duplex-link-op $n1 $n2 orient right-down
#$ns duplex-link-op $n5 $n2 orient right-up
#$ns duplex-link-op $n2 $n3 orient right
#$ns duplex-link-op $n4 $n3 orient left-down
#$ns duplex-link-op $n6 $n3 orient left-up


#Setup TCP connection 1
# n1 send TCP -> n4
set tcp1 [new Agent/$tcp_variant1]
$ns attach-agent $n1 $tcp1
## n4 'sink' send ACK packets -> n1
set sink1 [new Agent/TCPSink]
$ns attach-agent $n4 $sink1
$ns connect $tcp1 $sink1
$tcp1 set fid_ 1

#Setup a FTP over TCP connection1
set ftp1 [new Application/FTP]
$ftp1 attach-agent $tcp1
$ftp1 set type_ FTP

#Setup TCP connection 2
# n5 send TCP -> n6
set tcp2 [new Agent/$tcp_variant2]
$ns attach-agent $n5 $tcp2
# n6 'sink' send ACK packets -> n5
set sink2 [new Agent/TCPSink]
$ns attach-agent $n6 $sink2
$ns connect $tcp2 $sink2
$tcp2 set fid_ 2

#Setup a FTP over TCP connection2
set ftp2 [new Application/FTP]
$ftp2 attach-agent $tcp2
$ftp2 set type_ FTP


#Setup a UDP connection
# CBR: n2 -> n3
set udp [new Agent/UDP]
$ns attach-agent $n2 $udp
set null [new Agent/Null]
$ns attach-agent $n3 $null
$ns connect $udp $null
$udp set fid_ 3

#Setup a CBR over UDP connection
set cbr [new Application/Traffic/CBR]
$cbr set type_ CBR
$cbr set packet_size_ 1000
$cbr set rate_ $cbr_rate
$cbr set random_ false
$cbr attach-agent $udp


#Schedule events for the CBR and FTP agents
$ns at $tcp1_start "$ftp1 start"
$ns at $tcp2_start "$ftp2 start"
$ns at $tcp1_stop "$ftp1 stop"
$ns at $tcp2_stop "$ftp2 stop"
$ns at $cbr_start "$cbr start"
$ns at $cbr_stop "$cbr stop"

#Detach tcp and sink agents (not really necessary)
# $ns at 4.5 "$ns detach-agent $n0 $tcp ; $ns detach-agent $n3 $sink"

#Call the finish procedure after $finish_time seconds of simulation time
$ns at $duration "finish"

#Print CBR packet size and interval
puts "TCP_VARIANT1 = $tcp_variant1, TCP_VARIANT2 = $tcp_variant2, CBR = $cbr_rate"

#Run the simulation
$ns run
