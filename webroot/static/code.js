const listContainer = document.querySelector('#service-list');
let servicesRequest = new Request('/service');
var set_delay = 5000,
    callout = function () {
        $.ajax({
            fetch(servicesRequest).then(function(response) { return response.json(); })
                .then(function(serviceList) {
                    listContainer.innerHTML = "";
                    serviceList.forEach(service => {
                        var li = document.createElement("li");
                        li.appendChild(document.createTextNode(service.name + ' : ' + service.url + " : " + service.status));
                        listContainer.appendChild(li);
                    });
                })
        }).done(function (response) {

            })
            .always(function () {
                setTimeout(callout, set_delay);
            });
    };

callout();

const saveButton = document.querySelector('#post-service');
saveButton.onclick = evt => {
    let serviceName = document.querySelector('#service-name').value;
    let serviceUrl = document.querySelector('#service-url').value;

    fetch('/service', {
    method: 'post',
    headers: {
    'Accept': 'application/json, text/plain, */*',
    'Content-Type': 'application/json'
    },
  body: JSON.stringify({url:serviceUrl, name:serviceName })
}).then(res=> location.reload());
}

const deleteButton = document.querySelector('#delete-service');
deleteButton.onclick = evt => {
    let serviceName = document.querySelector('#delete-service-name').value;
    fetch('/service?name='+serviceName, {
        method: 'delete',
        headers: {
            'Accept': 'application/json, text/plain, */*',
            'Content-Type': 'application/json'
        }
    }).then(res=> location.reload());
}