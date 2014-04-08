var twitter = require('ntwitter');
var express = require('express.io');
var fs = require('fs');
var app = express();
var jade = require('jade');
app.set('views',__dirname);
app.set('view engine','jade');
app.http().io();
app.listen(8888);
var conf = require('./conf.json');

var arrayOfTags = ['gigtag','selfie'];
var myTwitStream;

var t = new twitter({
  consumer_key: conf.consumer_key,
  consumer_secret: conf.consumer_secret,
  access_token_key: conf.access_token_key,
  access_token_secret: conf.access_token_secret
});

app.use('/public', express.static(__dirname + '/public'));

//app.use('/text', express.static(__dirname + '/tweets'));

app.get('/gig/:tag/shapes', function(req,res){
  res.render('Shapes.jade',{tag:req.params.tag});
});

app.get('/gig/:tag', function(req,res){
  res.render('Tweets.jade',{tag:req.params.tag});
});

app.get('/gig/:tag/colors', function(req,res){
  res.render('Colors.jade',{tag:req.params.tag});
});

app.get('/gig/:tag/text', function(req,res){
  res.sendfile(__dirname + '/tweets/'+req.params.tag+".txt")
});

app.get('/mod/:tag', function(req,res){
  res.render('moderation.jade',{tag:req.params.tag});
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

app.io.route('moderate',function(req){
  app.io.sockets.in(req.data.tag.toLowerCase()).emit('moderatedTweet',req.data);
  writeToFile(req.data);
});

app.use(function(err,req,res,next){
  console.log(err);
  res.send(err.status,err.status + " There has been some sort of error")
});

function writeToFile(data){
  //data contains .user, .text, and .tag
  var filename = __dirname + '/tweets/'+data.tag+".txt";
  var strToWrite = '@'+data.user+":"+data.text+"\n";
  fs.appendFile(filename,strToWrite,function(err){
    if(err){
      console.log("err" + err);
    }
  });
}

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
        if(tweet.toLowerCase().indexOf(tag.toLowerCase()) != -1)
          app.io.sockets.in(tag.toLowerCase()).emit('tweet',data);
      });
    });
    myTwitStream = stream;
  });
}
trackTags(arrayOfTags);