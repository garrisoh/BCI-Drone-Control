var ardrone = require('ar-drone');
var net = require('net');

// create the drone and connect
var drone = ardrone.createClient();

/**
 *  Processes a received command and sends a control command
 *  to the Parrot Ar.drone.  This function is called when new
 *  data arrives on the server.  Basic commands such as takeoff,
 *  land, and hover are single word commands.  Commands that
 *  require a speed such as up or left must consist of the command
 *  followed by a space and a double value between 0 and 1.
 *
 *  @param command A Buffer containing the desired command
 */
function processCommand(command) {
	var cmd = null;
	
	// convert buffer to a string
	if (command instanceof Buffer) {
		cmd = command.toString();
	} else if (typeof(command) === 'string') {
		cmd = command;
	} 
	
	// remove carriage return and newline chars
	cmd = cmd.replace('\r', '');
	cmd = cmd.replace('\n', '');
	
	// separate command by spaces
	var cmdFields = cmd.split(' ');

	// handle the command
	if (cmdFields[0] === 'takeoff') {
		// trim before takeoff
		drone.ftrim();
		drone.takeoff();
	} else if (cmdFields[0] === 'land') {
		drone.land();
	} else if (cmdFields[0] === 'hover') {
		drone.stop();
	} else if (cmdFields[0] === 'up') {
		drone.up(parseFloat(cmdFields[1]));
	} else if (cmdFields[0] === 'down') {
		drone.down(parseFloat(cmdFields[1]));
	} else if (cmdFields[0] === 'left') {
		drone.left(parseFloat(cmdFields[1]));
	} else if (cmdFields[0] === 'right') {
		drone.right(parseFloat(cmdFields[1]));
	} else if (cmdFields[0] === 'forward') {
		drone.front(parseFloat(cmdFields[1]));
	} else if (cmdFields[0] === 'backward') {
		drone.back(parseFloat(cmdFields[1]));
	} else if (cmdFields[0] === 'clockwise') {
		drone.clockwise(parseFloat(cmdFields[1]));
	} else if (cmdFields[0] === 'counterClockwise') {
		drone.counterClockwise(parseFloat(cmdFields[1]));
	} else {
		console.log('Invalid Command: ' + cmd);
	}
}


// create a server listening on port 5678
net.createServer(function (client) {
	console.log('Connected');

	// set disconnected callback
	client.on('end', function () {
		console.log('Disconnected');
	});
	
	// set data callback
	client.on('data', function (data) {
		processCommand(data);
	});
}).listen(5678);
