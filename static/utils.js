(function (doc) {
    const visitor_id = function () {
        return 'xxxxxxxx-xxxx-4xxx-xxxx-xxxxxxxxxxx'.replace(/[x]/g, function (c) {
            var r = Math.random() * 16 | 0,
                v = c === 'x' ? r : (r & 0x3 | 0x8);
            return v.toString(16);
        });
    }

    const shadow = '<div id="loading"\n' +
        '     style="z-index: 999999;left:0;top:0;width: 100vw;height: 100vh;position: absolute;background: #141414; opacity: 0.9;text-align: center">\n' +
        '    <div style="margin-top: 45vh;width: 100vw; ">\n' +
        '        <div id="general-indicator">\n' +
        '            <div class="mdui-spinner"></div>\n' +
        '        </div>\n' +
        '        <div id="progress-indicator" class="mdui-typo" style="width: 35vw; margin-left: calc(32.5vw)">\n' +
        '            <div class="mdui-progress">\n' +
        '                <div id="progress-bar" class="mdui-progress-determinate" style="width: 30%;"></div>\n' +
        '            </div>\n' +
        '            <blockquote style="text-align: left">\n' +
        '                <footer>\n' +
        '                    <p id="step-name"></p>\n' +
        '                </footer>\n' +
        '            </blockquote>\n' +
        '        </div>\n' +
        '    </div>\n' +
        '</div>'


    const hash = function(t) {
        if (null == t) return - 1;
        try {
            let a = 0;
            let e = 0;
            for (; e < t.length; e++) {
                const n = t.charCodeAt(e);
                n < 128 && n > 48 && (a += n)
            }
            return a
        } catch(t) {
            return - 2
        }
    }

    const IDENTITY = visitor_id()


    let shadowLayer = null
    let generalIndicator = null
    let progressIndicator = null

    let progressBar = null
    let stepName = null

    const initShadowLayer = function () {
        if(shadowLayer == null) {
            $('body').append(shadow)
            shadowLayer = $('#loading')
            progressBar = $('#progress-bar')
            generalIndicator = $("#general-indicator")
            stepName = $("#step-name")
            progressIndicator = $("#progress-indicator")
            progressIndicator.hide()
            shadowLayer.hide()
            mdui.mutation()
            console.log("shadow layer injected")
        }
    }


    doc.dataPost = function(path, submitData, callback){
        initShadowLayer()

        shadowLayer.show()

        const data = {}
        data.requestId = visitor_id()
        data.identifier = IDENTITY
        data.requestTime = Date.now()
        data.data = submitData

        const serialized = JSON.stringify(data)

        let requester = new XMLHttpRequest();
        requester.open("POST", path, true)
        requester.setRequestHeader('content-type', 'application/json; charset=utf-8');
        requester.setRequestHeader('check-sum', "" + hash(serialized));

        requester.send(serialized)

        requester.onreadystatechange = function () {
            if (requester.readyState === 4) {
                try {
                    let res = JSON.parse(requester.responseText);
                    if(!res.success){
                        mdui.dialog({
                            title: 'Error',
                            content: res.errorMessage,
                        });
                        progressIndicator.hide()
                        generalIndicator.show()
                        shadowLayer.hide()
                        callback(false, null)
                        return
                    }

                    if(res.isTracingTask){
                        generalIndicator.hide()
                        progressIndicator.show()

                        let percentage = res.data.currStep / (res.data.totalStep + 1)
                        if(percentage > 1){
                            percentage = 1
                        }
                        let width = "" + percentage * 100 + "%"
                        progressBar.css("width",width)
                        stepName.text("" + res.data.currStep + "/" +  res.data.totalStep + " " + res.data.currStepName)

                        setTimeout(function () {
                            doc.dataPost("/trace",{"traceId": res.data.traceId},callback)
                        },444)
                    }else{
                        progressIndicator.hide()
                        generalIndicator.show()
                        shadowLayer.hide()
                        callback(true, res.data)
                    }


                } catch (e) {
                    mdui.dialog({
                        title: 'Error',
                        content: "Failed to retrieve server response",
                    });
                    progressIndicator.hide()
                    generalIndicator.show()
                    shadowLayer.hide()
                    callback(false, null)
                }
            }
        }

    }

})(document)