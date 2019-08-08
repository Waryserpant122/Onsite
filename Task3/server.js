var mysql = require('mysql');
const express = require('express');
const app = express();
//app.use(bodyParser.json());    
const port = 3000
var timestamp;
var i=0;
var fs =require('fs');
var bodyParser = require('body-parser')
app.use(bodyParser.urlencoded({ extended: true }));

app.listen(port, () => console.log(`listening on port ${port}`))

var con = mysql.createConnection({
  host: "localhost",
  user: "sammy",
  password: "password",
  database: "mydb"
});

//delete table
/*
var sql = "DROP TABLE wayback1";
  con.query(sql, function (err, result) {
    if (err) throw err;
    console.log("Table deleted");
 });


//create
var sqlab = "CREATE TABLE wayback1 (id int primary key auto_increment, timestamp VARCHAR(255), sourcefile VARCHAR(255))";
  con.query(sqlab, function (err, result) {
    if (err) throw err;
    console.log("Table created");
 });
*/

app.get('/',function(req,res){
	res.sendFile(__dirname + "/index.html");
})

function copyFile(source, target, cb) {
  var cbCalled = false;

  var rd = fs.createReadStream(source);
  rd.on("error", function(err) {
    done(err);
  });
  var wr = fs.createWriteStream(target);
  wr.on("error", function(err) {
    done(err);
  });
  wr.on("close", function(ex) {
    done();
  });
  rd.pipe(wr);

  function done(err) {
    if (!cbCalled) {
      cb(err);
      cbCalled = true;
    }
  }
}
app.get('/submit',function(req,res){
	i++;
   timestamp= req.query.timestamp;
   copyFile('index.html', 'files/index'+i+'.html', (err) => {
  if (err) throw err;
 // console.log('source.txt was copied to destination.txt');
  });
    var j="index"+i+".html"
   //console.log(timestamp);
  // console.log(j);
   var sql = "INSERT INTO wayback1 (timestamp,sourcefile) VALUES (?,?)";
   con.query(sql, [timestamp,j], function (err, result) {
    if (err) throw err;
   // console.log("1 record inserted");
     con.query("SELECT * FROM wayback1", function (err, result, fields) {
    if (err) throw err;
   console.log(result);
    
  });
  });
  
 });

app.get('/view1',function(req,res)
{
    res.sendFile(__dirname + "/files/index1.html");
});

app.get('/view2',function(req,res)
{
    res.sendFile(__dirname + "/files/index2.html");
});

app.get('/view3',function(req,res)
{
    res.sendFile(__dirname + "/files/index3.html");
});

app.get('/view4',function(req,res)
{
    res.sendFile(__dirname + "/files/index4.html");
});

app.get('/view5',function(req,res)
{
    res.sendFile(__dirname + "/files/index5.html");
});

app.get('/view6',function(req,res)
{
    res.sendFile(__dirname + "/files/index6.html");
});

app.get('/view7',function(req,res)
{
    res.sendFile(__dirname + "/files/index7.html");
});

app.get('/view8',function(req,res)
{
    res.sendFile(__dirname + "/files/index8.html");
});

app.get('/view9',function(req,res)
{
    res.sendFile(__dirname + "/files/index9.html");
});

app.get('/view10',function(req,res)
{
    res.sendFile(__dirname + "/files/index10.html");
});

app.get('/view11',function(req,res)
{
    res.sendFile(__dirname + "/files/index11.html");
});

app.get('/view12',function(req,res)
{
    res.sendFile(__dirname + "/files/index12.html");
});

app.get('/view13',function(req,res)
{
    res.sendFile(__dirname + "/files/index13.html");
});

app.get('/view14',function(req,res)
{
    res.sendFile(__dirname + "/files/index14.html");
});

app.get('/view15',function(req,res)
{
    res.sendFile(__dirname + "/files/index15.html");
});

app.get('/view16',function(req,res)
{
    res.sendFile(__dirname + "/files/index16.html");
});

app.get('/view17',function(req,res)
{
    res.sendFile(__dirname + "/files/index17.html");
});

app.get('/view18',function(req,res)
{
    res.sendFile(__dirname + "/files/index18.html");
});

app.get('/view19',function(req,res)
{
    res.sendFile(__dirname + "/files/index19.html");
});
  /* let sql = `create table if not exists wayback(
                          id int primary key auto_increment,
                          timestamp varchar(255)not null,
                          sourcefile varchar(255) not null default 0
                      )`;
 // var sql = "CREATE TABLE customers (id int primary key auto_increment, time VARCHAR(255), sourcefile VARCHAR)";
  con.query(sql, function (err, result) {
    if (err) throw err;
    console.log("Table created");
  });
  */

