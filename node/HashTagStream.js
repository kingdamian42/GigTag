var twitter = require('ntwitter');
var app  = require('express.io')();
var jade = require('jade');
app.set('views',__dirname);
app.set('view engine','jade');
app.http().io();
app.listen(8888);
var conf = require('./conf.json');

var arrayOfTags = [];
var myTwitStream;

var t = new twitter({
  consumer_key: conf.consumer_key,
  consumer_secret: conf.consumer_secret,
  access_token_key: conf.access_token_key,
  access_token_secret: conf.access_token_secret
});

app.get('/gig/:tag', function(req,res){
  res.render('HashTagStreamTest.jade',{tag:req.params.tag});
});

app.io.route('create',function(req){
  req.socket.join(req.data); //makes it only read the hashtag for req.data
});

//Adds a hashtag to search
app.io.route('add',function(req){
  addTag(req);
});
app.io.route('remove',function(req){
  removeTag(req);
});

function sendTags(){
  app.io.emit('tags',{'tags': arrayOfTags});
}

function addTag(req){
  if(arrayOfTags.indexOf() == -1){
    arrayOfTags.push(req.data.hashtag);
    trackTags(arrayOfTags);
  }
  req.io.emit('tags',{'tags': arrayOfTags});
}

function removeTag(req){
  var index = arrayOfTags.indexOf(req);
  if(index != -1){
    if(arrayOfTags.length == 1) arrayOfTags = [];
    else  arrayOfTags.splice(index,1);
    trackTags(arrayOfTags);
  }
  req.io.emit('tags',{'tags': arrayOfTags});
}

function trackTags(array){
  if(myTwitStream != null){
    myTwitStream.destroy();
  }
  if(array.length == 0) return;
  t.stream('statuses/filter', {'track': array}, function(stream) {
    stream.on('data', function(data) {
      var tweet = data.text;
      array.forEach(function(tag){
        if(tweet.indexOf(tag) != -1)
          app.io.sockets.in(tag).emit('tweet',data);
      });
    });
    myTwitStream = stream;
  });
}
trackTags(arrayOfTags);