var keyBuffer = "";
var timeoutID = null;

$(document).keypress(function (event) {
    if ((event.which === 10 || event.which === 13) && keyBuffer.length > 3) {
        event.preventDefault();
        location.pathname = "/barcode/" + keyBuffer;
        keyBuffer = "";
        if(timeout)
        {
            clearTimeout(timeoutID);
        }
        return;
    }

    var char = String.fromCharCode(event.which);
    if(char.match(/[0-9]/))
    {
        event.preventDefault();
        keyBuffer += char;
        if(timeoutID)
        {
            clearTimeout(timeoutID)
        }
        timeoutID = setTimeout(clearBuffer, 100)
    }
});

function clearBuffer() {
    keyBuffer = "";
    timeoutID = null;
}
