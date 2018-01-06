$(document).foundation();

$(function () {

    var keyBuffer = "";
    var timeoutID = null;
    var input = $('#barcode-input');

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
        input.val(input.val() + keyBuffer);
        keyBuffer = "";
        timeoutID = null;
    }

    $('#barcode-form').on('submit', function (e) {
        e.preventDefault();
        var barcode = input.val();
        input.val("");
        if (barcode.match(/^\d{10,13}$/))
        {
            location.pathname = "/barcode/" + barcode;
        }
    })
})
