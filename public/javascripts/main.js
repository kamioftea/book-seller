$(document).foundation();

$(function () {

    var keyBuffer = "";
    var timeoutID = null;
    var input = $('#barcode-input');

    $(document).keypress(function (event) {
        if ((event.which === 10 || event.which === 13) && keyBuffer.length >= 10) {
            event.preventDefault();
            location.pathname = "/barcode/" + keyBuffer;
            keyBuffer = "";
            if(timeout)
            {
                clearTimeout(timeoutID);
            }
            return;
        }

        if ((event.which === 10 || event.which === 13) && input.val().length) {
            event.preventDefault();
            $('#barcode-form').trigger('submit');
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
        var barcode = input.val().replace(/-/g, "");
        input.val("");
        console.log(barcode);
        if (barcode.match(/^\d{10,13}$/))
        {
            location.pathname = "/barcode/" + barcode;
        }
    })
});
