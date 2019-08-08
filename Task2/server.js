const express = require('express')
const session = require('express-session')
const bodyParser = require('body-parser')
 
const captchaUrl = '/captcha.jpg'
const captchaSessionId = 'captcha'
const captchaFieldName = 'captcha'
var port = 2000; 



const captcha = require('svg-captcha-express').create({
    cookie: 'captcha'

})
 
const app = express()
app.listen(port, function () {
    console.log("Listening on port " + port);
});

app.use(session({
    secret: 'illuminati',
    resave: false,
    saveUninitialized: true,
}))
app.use(bodyParser.urlencoded({ extended: false }))
// console.log(cookie.captcha);
app.get(captchaUrl, captcha.image())
 //console.log(captcha);
 
app.get('/', (req, res) => {
    console.log(req.session.captcha)
    //console.log(cookie.captcha);
    res.type('html')
    res.end(`
        <p>Delta Inductions Onsite Backend Task 1</p>
        <img src="${ captchaUrl }"/>
        <form action="/login" method="post">
            <input autocomplete="off" type="text" name="${'captcha'}"/>
            <input type="submit"/>
        </form>
    `)
    


})
 
//console.log(req.session)
 
app.post('/login', (req, res) => {
    console.log(req.session)
    res.type('html')
    res.end(`
        <p>The Captcha Entered is: ${ captcha.check(req, req.body['captcha']) }</p>
    `)
   // res.redirect('back');
})