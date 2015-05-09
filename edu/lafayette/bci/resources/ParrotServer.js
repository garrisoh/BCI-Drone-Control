var ardrone = require('ar-drone');
var net = require('net');

// create the drone and connect
var drone = ardrone.createClient();

var takeoff = false;

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
	var cmdstr = null;
	
	// convert buffer to a string
	if (command instanceof Buffer) {
		cmdstr = command.toString();
	} else if (typeof(command) === 'string') {
		cmdstr = command;
	} 
	
	// remove carriage returns
	cmdstr = cmdstr.split('\r').join('');

	// split around newline chars, remove empty string at end
	var cmds = cmdstr.split('\n');
	if (cmds[cmds.length - 1] === '') {
		cmds.pop();
	}

	// Iterate through each command
	for (var i = 0; i < cmds.length; i++) {
		// separate command by spaces
		var cmdFields = cmds[i].split(' ');

		// handle the command
		if (cmdFields[0] === 'takeoff' && !takeoff) {
			// trim before takeoff
			drone.ftrim();
			drone.takeoff();
			takeoff = true;
		} else if (cmdFields[0] === 'land' && takeoff) {
			drone.land();
			takeoff = false;
		} else if (cmdFields[0] === 'hover' && takeoff) {
			drone.stop();
		} else if (cmdFields[0] === 'up' && takeoff) {
			drone.up(parseFloat(cmdFields[1]));
		} else if (cmdFields[0] === 'down' && takeoff) {
			drone.down(parseFloat(cmdFields[1]));
		} else if (cmdFields[0] === 'left' && takeoff) {
			drone.left(parseFloat(cmdFields[1]));
		} else if (cmdFields[0] === 'right' && takeoff) {
			drone.right(parseFloat(cmdFields[1]));
		} else if (cmdFields[0] === 'forward' && takeoff) {
			drone.front(parseFloat(cmdFields[1]));
		} else if (cmdFields[0] === 'backward' && takeoff) {
			drone.back(parseFloat(cmdFields[1]));
		} else if (cmdFields[0] === 'clockwise' && takeoff) {
			drone.clockwise(parseFloat(cmdFields[1]));
		} else if (cmdFields[0] === 'counterClockwise' && takeoff) {
			drone.counterClockwise(parseFloat(cmdFields[1]));
		} else if (!takeoff) {
			console.log('Must takeoff to receive command: ' + cmds[i]);
		} else if (takeoff && cmdFields[0] === 'takeoff') {
			console.log('Drone has already taken off: ' + cmds[i]);
		} else {
			console.log('Invalid Command: ' + cmds[i]);
		}
	}
}


// create a server listening on port 5678
net.createServer(function (client) {
	console.log('Connected');

	// set disconnected callback
	client.on('end', function () {
		console.log('Disconnected');
		drone.stop();
		drone.land();
	});
	
	// set data callback
	client.on('data', function (data) {
		processCommand(data);
	});
}).listen(5678);
