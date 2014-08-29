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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import javassist.util.proxy.Proxy;
import org.apache.commons.io.IOUtils;
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
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;

/**
 * Your first Wisdom Controller.
 */
@Controller
public class NewsExtensionController extends DefaultController implements MonitorExtension{

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
    @Route(method = HttpMethod.GET, uri = "/monitor/news/create")
    public Result create(){
        fakedata();
//        NewsArticle newbie = new NewsArticle();
//        newbie.setAuthor(author);
//        newbie.setAuthor("bob");
//        newbie.setContent("stuff");
//        newbie.setTitle("new title");
//        newbie.setDateCreated("today");
//        newsArticleCrud.save(newbie);
        return ok();
    }

    @Authenticated("Monitor-Authenticator")
    @Route(method = HttpMethod.DELETE, uri = "/news/list/{id}")
    public Result delete(@Parameter("id") String id){
        if(newsArticleCrud.exists(id)){
            newsArticleCrud.delete(id);
        }
        return ok();
    }

    @Authenticated("Monitor-Authenticator")
    @Route(method = HttpMethod.POST, uri = "/news/list/{id}")
    public Result update(@Parameter("id") String id){
        if(newsArticleCrud.exists(id)){
            NewsArticle articleToUpdate = newsArticleCrud.findOne(id);
            articleToUpdate.setAuthor("bob");
            articleToUpdate.setContent("stuff");
            articleToUpdate.setTitle("new title");
            articleToUpdate.setDataModified("today");
        }
        return ok();
    }

    @Route(method = HttpMethod.GET, uri = "/news/list")
    public Result get() {

        List<NewsArticle> list = new LinkedList<NewsArticle>();
        for (NewsArticle article : newsArticleCrud.findAll()) {
            list.add(article);
        }
       return ok(list).json();

    }

    public void fakedata(){
        NewsArticle articleToUpdate = new NewsArticle();
        for(int i = 0; i <5;i++){
            articleToUpdate.setAuthor("bob"+i);
            articleToUpdate.setContent("stuff"+i);
            articleToUpdate.setTitle("new title"+i);
            articleToUpdate.setDataModified("today"+i);
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
