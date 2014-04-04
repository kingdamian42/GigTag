var channel = '#IRCChannelName';
var myIRCbot = new irc.Client(
  'chat.freenode.net', 'MyIRCBotName', {
  channels: channel
});

//read a message and echo it back to the room
myIRCbot.addListener("message", function(from, to, message) {
  myIRCbot.say(channel,from + ' said ' + message);
});