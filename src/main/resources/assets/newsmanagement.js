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
    });
    //load article list
    load();

    $('#cancel').hide().click(function () {
        resetForm();
    });

    $('#edit').hide().click(function () {
        submitForm();
    });

    $('#create').click(function () {
        submitForm();
    });


});

function submitForm() {
    var data = $('form').serialize();
    var addy;
    var act="post";
    var $genid = $("#genid");
    if ($genid.val() == "void") {
        addy = "/monitor/news/create";
    }
    else {

        addy = "http://" + window.location.host + "/news/list?id=" + encodeURIComponent($genid.val());
    }
    if (checkInputs()) {
        create(addy, data, act);
    }
}

/* Check is the input fields that are required are not blank */
function checkInputs() {
    var result=true;
    var $articletitle = $("#articletitle");
    if($articletitle.val()==""){
        $("#titlegroup").addClass("has-error has-feedback");
        $articletitle.attr("placeholder", "A title is required");
        result=false;
    } else {
        $("#titlegroup").removeClass("has-error has-feedback");
        $articletitle.attr("placeholder", "required");
    }

    var $articlecontent = $("#articlecontent");
    if($articlecontent.val()==""){
        $("#contentgroup").addClass("has-error has-feedback");
        $articlecontent.attr("placeholder", "Content is required");
        result=false;
    }else {
        $("#contentgroup").removeClass("has-error has-feedback");
        $articlecontent.attr("placeholder", "Write your content here..");
    }
    return result;
}

/* create a new extension from a json file, calls the upload method*/
function create(address, data, action) {
    $.ajax({
        type: action,
        url: address,
        data: data
    }).done(function (data) {
        // console.log(data);
        //if error message display
        if (data.error) {
            //need an error msg?
            }
        load();
        resetForm();
    });
}

/*remove the selected article based on the database id number */
function remove(ext) {
    $.ajax({
        url: "http://" + window.location.host + "/news/list?id=" + encodeURIComponent(ext),
        type: 'DELETE',
        complete: function () {
            $("#error-msg").html("").removeClass("alert-success").removeClass("alert-danger").removeClass("alert-warning");
            load();
        }
    });
}
/* resets form to initial state */
function resetForm(){

    $('#formtitle').text("Create News Article");
    $("#idheading").text("");
    $("#articletitle").val("");
    $("#articlecontent").val("");
    $("#articleauthor").val("");
    $("#create").show();
    $("#edit").hide();
    $("#cancel").hide();
    $("#demo").collapse('show');

}
/* retrieve a specific article from database */
function retrieveRecord(idnumber) {

    var url = "http://" + window.location.host + "/news/article?id=" + encodeURIComponent(idnumber);
    $.getJSON(url, function (result) {


        $('#formtitle').text("Edit News Article");
        $("#idheading").text("Article id : " + result.id);
        $("#genid").val(result.id);
        $("#articletitle").val(result.title);
        $("#articlecontent").val(result.content);
        $("#articleauthor").val(result.author);
        $("#create").hide();
        $("#edit").show();
        $("#cancel").show();
        $("#demo").collapse('show');

    });
}



/*create actions available for each article, based on the database id number*/
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
        // update(ext)
        //  $('#basicModal').modal();
        retrieveRecord(ext);
    });
    inner.append(uninstall);
    inner.append(extupdate);
    bar.append(inner);
    return $("<td></td>").append(bar);
}

/* list all available articles in a table */
function createTable(data) {

    $("#ext-table-body").empty();
    $.each(data, function (index, value) {
        console.log(index, value);
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


        var content = $("<div></div>").attr("id", "collapse" + newsId).addClass("articlecontent col-xs-12 collapse").html(value.content).appendTo(info);
        //second column of table contains the version
        var author = $("<td></td>").html(value.author);
        var version = $("<td></td>").html(prettyDate(value.dateCreated));
        var modified = $("<td></td>").html(prettyDate(value.dateModified));

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
/* format the date */
function prettyDate(date) {
    if (date == null) {
        return "";
    }
    var d = new Date(date);
    return $.format.prettyDate(d);
}

/*load the list of articles as json */
function load() {
    $.get("http://" + window.location.host + "/news/list").success(createTable)
}

/**
 * @return {boolean}
 */
function HasSubstring(string, substring) {
    return string.indexOf(substring) > -1;

}
