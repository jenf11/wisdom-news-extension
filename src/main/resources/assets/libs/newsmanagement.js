$(document).ready(function () {
    // Table Sorter.
    $("#table").tablesorter(
        {
            // We have overridden this style.
            theme: 'dropbox',

            sortList: [
                [0, 0],
                [1, 0],
                [2, 0],
                [3, 0]


            ],

            // header layout template; {icon} needed for some themes
            headerTemplate: '{content}&nbsp;{icon}',

            // initialize column styling of the table
            widgets: ["columns"],
            widgetOptions: {
                // change the default column class names
                // primary is the first column sorted, secondary is the second, etc
                columns: [ "primary", "secondary", "third", "fourth" ]
            }
        });
    //Tooltip
    $("a").tooltip();
    $("#refresh").click(function () {
        load();
        $("#error-msg").html("").removeClass("alert-success").removeClass("alert-danger").removeClass("alert-warning");
    });
    //add a new extension
    $("#add").click(function () {
        create($('#url').val());
        $("#error-msg").html("").removeClass("alert-success").removeClass("alert-danger").removeClass("alert-warning");
        $("#url").val("");

    });
    //load extension list
    load();

});

/* create a new extension from a json file, calls the upload method*/
function create(ext) {
    var url = "http://" + window.location.host + "/registry/upload";
    // var url = /*[[${#routes.route('upload')}]]*/ null;
    $.ajax({
        type: "POST",
        url: url,
        data: { url: ext }
    }).done(function (data) {
        // console.log(data);
        //if error message display
        if (data.error) {
            $("#error-msg").html(data.error + " " + data.reason).addClass("alert-danger").removeClass("alert-success").removeClass("alert-warning");
        }
        else if (data.updated) {
            $("#error-msg").html("The " + data.name + " extension has been updated.").removeClass("alert-danger").removeClass("alert-success").addClass("alert-warning");
        } else {
            $("#error-msg").html("The " + data.name + " extension has been added.").removeClass("alert-danger").removeClass("alert-warning").addClass("alert-success");
        }
        load();
    });
}

/*remove the selected extension based on the database id number */
function remove(ext) {
    $.ajax({
        url: "http://" + window.location.host + "/registry/list/" + encodeURIComponent(ext),
        type: 'DELETE',
        complete: function (result) {
            $("#error-msg").html("").removeClass("alert-success").removeClass("alert-danger").removeClass("alert-warning");
            load();
        }
    });
}

/*remove the selected extension based on the database id number */
function update(ext) {
    $.ajax({
        url: "http://" + window.location.host + "/registry/list/" + encodeURIComponent(ext),
        type: 'POST',
        complete: function (result) {
            $("#error-msg").html("").removeClass("alert-success").removeClass("alert-danger").removeClass("alert-warning");
            load();
        }
    });
}

/*create actions available for each extension so far that iis just delete, based on the database id number*/
function getActionBarForExtension(ext) {
    var bar = $("<div></div>").addClass("bundle-action-bar pull-right").addClass("btn-toolbar").attr("role",
        "toolbar");
    var inner = $("<div></div>").addClass("btn-group");
    var uninstall = $("<button type=\"button\" class=\"btn btn-default btn-xs\"><span class=\"glyphicon glyphicon-remove\"></span></button>");
    uninstall.click(function () {
        remove(ext)
    });
    var extupdate = $("<button type=\"button\" class=\"btn btn-default btn-xs\"><span class=\"glyphicon glyphicon-repeat\"></span></button>");
    extupdate.click(function () {
        update(ext)
    });
    inner.append(uninstall);
    inner.append(extupdate);
    bar.append(inner);
    return $("<td></td>").append(bar);
}

/* list all available extension in a table */
function writeExtensionData(data) {

    $("#ext-table-body").empty();
    $.each(data, function (index, value) {
        console.log(index,value);
        var newsId = value.id;
        if (HasSubstring(newsId, "#")) {
            newsId = newsId.replace("#", "-");
        }
        if (HasSubstring(newsId, ":")) {
            newsId = newsId.replace(":", "-");
        }

        //create a new row using the extensions key as a collasable link
        var tr = $("<tr></tr>");
        var info = $("<td></td>");
        info.append($("<a></a>")
            .attr("href", "#collapse" + newsId)
            .attr("data-toggle", "collapse")
            .html(value.title));


        var content = $("<div></div>").attr("id","collapse"+newsId).addClass("articlecontent col-xs-12 collapse").html(value.content).appendTo(info);


        //second column of table contains the version
            var author =  $("<td></td>").html(value.author);
            var version = $("<td></td>").html(value.dateCreated);
            var modified = $("<td></td>").html(value.dateModified);

        $(tr).append(info).append(author).append(version).append(modified).append(getActionBarForExtension(value.id));

        $("#ext-table-body").append(tr);
    });
    $("#ext-count").html(data.length);

    $("#filter").val("");
    $('table').trigger("update").filterTable({ // apply filterTable to all tables on this page
        filterSelector: '#filter',
        minRows: 2   //min number of rows before we filter
    });
}

/*load the list of extensions as json */
function load() {
    console.log(window.location.host);
    $.get("http://" + window.location.host + "/news/list").success(writeExtensionData)
}

/**
 * @return {boolean}
 */
function HasSubstring(string, substring) {
    return string.indexOf(substring) > -1;

}
