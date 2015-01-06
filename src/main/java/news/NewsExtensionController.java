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
import org.joda.time.Duration;
import org.wisdom.api.DefaultController;
import org.wisdom.api.annotations.*;
import org.wisdom.api.cache.Cache;
import org.wisdom.api.cache.Cached;
import org.wisdom.api.content.Json;
import org.wisdom.api.http.HeaderNames;
import org.wisdom.api.http.HttpMethod;
import org.wisdom.api.http.Result;
import org.wisdom.api.security.Authenticated;
import org.wisdom.api.templates.Template;
import org.wisdom.monitor.service.MonitorExtension;
import org.wisdom.orientdb.object.OrientDbCrud;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import javax.validation.constraints.NotNull;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * An extension that allows for managing news articles stored in an orientDB.
 */
@Controller
public class NewsExtensionController extends DefaultController implements MonitorExtension {

    /* the management view template */
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
     * Method that displays the management webpage.
     *
     * @return the management view template.
     */
    @Authenticated("Monitor-Authenticator")
    @Route(method = HttpMethod.GET, uri = "/monitor/news/manage")
    public Result manage() {
        return ok(render(manage));
    }

    /**
     * Creates a new news article and adds it to the database.
     *
     * @param title   of the article is required.
     * @param content of the article is required.
     * @param author  is optional.
     * @return ok.
     */
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
        } else {
            return ok(json.newObject().put("error", "An error has occurred:").put("reason",
                    "Please check that your post has content and title."));
        }

        return ok();
    }

    /**
     * Deletes a specified article from the database.
     *
     * @param id used to identify the article.
     * @return
     */
    @Authenticated("Monitor-Authenticator")
    @Route(method = HttpMethod.DELETE, uri = "/news/list")
    public Result delete(@NotNull @Parameter("id") String id) {

        newsArticleCrud.delete(id);
        //exsists allways returns false ?
        // if(newsArticleCrud.exists(id)){
//            newsArticleCrud.delete(id);
//        }

        return ok();
    }

    /**
     * TODO this method does not correctly check constraints .
     * Update the specified news article.
     *
     * @param id      identifies the article can't be null.
     * @param title   should not be empty.
     * @param content should not be empty.
     * @param author  is optional.
     * @return ok.
     */
    @Authenticated("Monitor-Authenticator")
    @Route(method = HttpMethod.POST, uri = "/news/list")
    public Result update(@NotNull @Parameter("id") String id,
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

    /**
     * Returns the specified news article.
     *
     * @param id used to identify the article.
     * @return ok.
     */
    @Route(method = HttpMethod.GET, uri = "/news/article")
    public Result getArticle(@NotNull @Parameter("id") String id) {
        System.out.println("get id" + id);
        NewsArticle articleToUpdate = newsArticleCrud.findOne(id);
        if (articleToUpdate != null) {
            return ok(articleToUpdate).json();
        } else return ok();
    }


    /**
     * Lists every article in the database.
     *
     * @return the list of articles as a json object.
     */
    @Route(method = HttpMethod.GET, uri = "/news/list")
    public Result get() {

        List<NewsArticle> list = new LinkedList<NewsArticle>();
        for (NewsArticle article : newsArticleCrud.findAll()) {

            list.add(article);
        }
        return ok(list).json();

    }

    @Requires
    Cache cache;

    /**
     * Generate a list of articles from the database. Listed in descending (newest to oldest) order.
     *
     * @param genNum required parameter used to limit the results returned. Can be larger than
     *               the actual number of objects in the database.
     * @return the list of objects found as a json structure.
     */
    @Route(method = HttpMethod.GET, uri = "/news/list/generated/{genNum}")
    public Result generate(@Parameter("genNum") int genNum) {
        String uri = context().request().uri();
        Result cached = (Result) cache.get(uri);
        if (cached != null  &&
                ! HeaderNames.NOCACHE_VALUE
                        .equalsIgnoreCase(context().header(HeaderNames.CACHE_CONTROL))) {
            return cached;
        }

        List<NewsArticle> list = new LinkedList<NewsArticle>();
        List<NewsArticle> query = newsArticleCrud.query(new OSQLSynchQuery<NewsArticle>("select *" +
                " from NewsArticle order by dateModified DESC limit " + genNum));
        list.addAll(query);
        Result result = ok(list).json();
        cache.set(uri, result, Duration.standardHours(1));
        return result;
    }

    /**
     * Creates the label in the wisdom monitor.
     *
     * @return name of label.
     */
    @Override
    public String label() {
        return "News Manager";
    }

    /**
     * Designates the path of the page to be displayed.
     *
     * @return the path.
     */
    @Override
    public String url() {
        return "/monitor/news/manage";
    }

    /**
     * Desginates the category in the wisdom monitor to display the above label.
     *
     * @return name of category.
     */
    @Override
    public String category() {
        return "Documentation";
    }
}
