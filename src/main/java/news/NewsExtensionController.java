/*
 * #%L
 * Wisdom-Framework
 * %%
 * Copyright (C) 2013 - 2014 Wisdom Framework
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package news;

import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import javassist.util.proxy.Proxy;
import org.apache.felix.ipojo.annotations.Requires;
import org.wisdom.api.DefaultController;
import org.wisdom.api.annotations.*;
import org.wisdom.api.content.Json;
import org.wisdom.api.http.HttpMethod;
import org.wisdom.api.http.Result;
import org.wisdom.api.security.Authenticated;
import org.wisdom.api.templates.Template;
import org.wisdom.monitor.service.MonitorExtension;
import org.wisdom.orientdb.object.OrientDbCrud;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Your first Wisdom Controller.
 */
@Controller
public class NewsExtensionController extends DefaultController implements MonitorExtension {

    /**
     * Injects a template named 'welcome'.
     */
    @View("welcome")
    Template welcome;
    @View("news/manageView")
    Template manage;

    @Model(value = NewsArticle.class)
    private OrientDbCrud<NewsArticle, String> newsArticleCrud;

    Class klass = Proxy.class;

    @Requires
    Json json;

    @Requires
    Validator validator;

    /**
     * The action method returning the welcome page. It handles
     * HTTP GET request on the "/" URL.
     *
     * @return the welcome page
     */
    @Route(method = HttpMethod.GET, uri = "/")
    public Result welcome() {
        return ok(render(welcome, "welcome", "Welcome to Wisdom Framework!"));
    }

    @Authenticated("Monitor-Authenticator")
    @Route(method = HttpMethod.GET, uri = "/monitor/news/manage")
    public Result manage() {
        return ok(render(manage));
    }

    @Authenticated("Monitor-Authenticator")
    @Route(method = HttpMethod.POST, uri = "/monitor/news/create")
    public Result create(@Parameter("title") String title,
                         @Parameter("content") String content,
                         @Parameter("author") String author) {

        NewsArticle newbie = new NewsArticle();
        Date dateobj = new Date();
        if (!title.isEmpty()) {
            newbie.setTitle(title);
        }
        if (!content.isEmpty()) {
            newbie.setContent(content);
        }
        newbie.setAuthor(author);
        newbie.setDateCreated(dateobj);
        newbie.setDateModified(dateobj);
        Set<ConstraintViolation<NewsArticle>> errors = validator.validate(newbie);

        if (errors.isEmpty()) {
            newsArticleCrud.save(newbie);
            System.out.println("saved");
        } else {
            System.out.println("omg");
            return ok(json.newObject().put("error", "An error has occurred:").put("reason",
                    "Please check that your post has content and title."));
        }

        return ok();
    }

    @Authenticated("Monitor-Authenticator")
    @Route(method = HttpMethod.DELETE, uri = "/news/list/{id}")
    public Result delete(@Parameter("id") String id) {

        newsArticleCrud.delete(id);
        //exsists allways returns false
        // if(newsArticleCrud.exists(id)){
//            newsArticleCrud.delete(id);
//        }

        return ok();
    }

    @Authenticated("Monitor-Authenticator")
    @Route(method = HttpMethod.POST, uri = "/news/list/{id}")
    public Result update(@Parameter("id") String id,
                         @Parameter("title") String title,
                         @Parameter("content") String content,
                         @Parameter("author") String author) {
        NewsArticle articleToUpdate = newsArticleCrud.findOne(id);
        if (articleToUpdate != null) {
            articleToUpdate.setAuthor(author);
            articleToUpdate.setContent(content);
            articleToUpdate.setTitle(title);
            Date dateobj = new Date();
            articleToUpdate.setDateModified(dateobj);
            newsArticleCrud.save(articleToUpdate);

        }
        return ok();
    }

    @Route(method = HttpMethod.GET, uri = "/news/article/{id}")
    public Result getArticle(@Parameter("id") String id) {
        System.out.println("get id" + id);
        NewsArticle articleToUpdate = newsArticleCrud.findOne(id);
        if (articleToUpdate != null) {
            return ok(articleToUpdate).json();
        } else return ok();
    }

    @Route(method = HttpMethod.GET, uri = "/news/list")
    public Result get() {

        List<NewsArticle> list = new LinkedList<NewsArticle>();
        for (NewsArticle article : newsArticleCrud.findAll()) {

            list.add(article);
        }
        return ok(list).json();

    }

    @Route(method = HttpMethod.GET, uri = "/news/list/generated/{genNum}")
    public Result generate(@Parameter("genNum") String genNum) {
        genNum = genNum;
        int num = Integer.valueOf(genNum);
        List<NewsArticle> list = new LinkedList<NewsArticle>();
        List<NewsArticle> query = newsArticleCrud.query(new OSQLSynchQuery<NewsArticle>("select *" +
                " from NewsArticle order by dateModified DESC limit " + num));
        list.addAll(query);
        return ok(list).json();
    }

    public void fakedata(int num) {
        NewsArticle articleToUpdate = new NewsArticle();
        DateFormat df = new SimpleDateFormat("dd/MM/yy HH:mm:ss");
        Date dateobj = new Date();
        for (int i = 0; i < num; i++) {
            articleToUpdate.setAuthor("bob" + i);
            articleToUpdate.setContent("stuff" + i);
            articleToUpdate.setTitle("new title" + i);
            articleToUpdate.setDateCreated(dateobj);
            articleToUpdate.setDateModified(dateobj);
            newsArticleCrud.save(articleToUpdate);
        }
    }


    /**
     * The action method that loads a json object via a url and adds it to the database. It handles
     * HTTP POST request on the "/upload" URL.
     *
     * @return json structure containing the new extension
     */


    @Override
    public String label() {
        return "News Manager";
    }

    @Override
    public String url() {
        return "/monitor/news/manage";
    }

    @Override
    public String category() {
        return "Documentation";
    }
}
