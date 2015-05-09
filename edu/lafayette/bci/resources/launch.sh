# launch video player, suppress output, and send to background
ffplay tcp://192.168.1.1:5555 >& /dev/null &

# start the drone script
node ParrotServer.js

# kill ffplay
pkill -x ffplay
