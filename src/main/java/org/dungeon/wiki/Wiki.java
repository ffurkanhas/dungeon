/*
 * Copyright (C) 2014 Bernardo Sulzbach
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.dungeon.wiki;

import org.dungeon.commands.IssuedCommand;
import org.dungeon.io.JsonObjectFactory;
import org.dungeon.io.Writer;
import org.dungeon.util.Matches;
import org.dungeon.util.Utils;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.util.ArrayList;
import java.util.List;

/**
 * The Wiki class. Loads the contents of the wiki.txt file and manages wiki articles.
 */
public abstract class Wiki {

  private static List<Article> articleList;

  private static void initialize() {
    articleList = new ArrayList<Article>();
    JsonObject jsonObject = JsonObjectFactory.makeJsonObject("wiki.json");
    for (JsonValue article : jsonObject.get("articles").asArray()) {
      JsonObject articleObject = article.asObject();
      articleList.add(new Article(articleObject.get("title").asString(), articleObject.get("content").asString()));
    }
  }

  /**
   * Searches the wiki and prints the matching contents to the screen.
   *
   * @param issuedCommand an IssuedCommand
   */
  public static void search(IssuedCommand issuedCommand) {
    if (articleList == null) {
      initialize();
    }
    if (issuedCommand.hasArguments()) {
      Matches<Article> matches = Utils.findBestMatches(articleList, issuedCommand.getArguments());
      if (matches.size() == 0) {
        Writer.writeString("No matches were found.");
      } else if (matches.size() == 1) {
        Writer.writeString(matches.getMatch(0).toString());
      } else {
        StringBuilder builder = new StringBuilder();
        builder.append("The following articles match your query:\n");
        for (int i = 0; i < matches.size(); i++) {
          builder.append(toArticleListingEntry(matches.getMatch(i))).append("\n");
        }
        builder.append("Be more specific.");
        Writer.writeString(builder.toString());
      }
    } else {
      writeArticleList();
    }
  }

  /**
   * Writes the article count and a list with the titles of the {@code Articles} in the {@code articleList}.
   */
  private static void writeArticleList() {
    StringBuilder builder = new StringBuilder();
    builder.append("The wiki has the following ").append(articleList.size()).append(" articles:\n");
    for (Article article : articleList) {
      builder.append(toArticleListingEntry(article)).append("\n");
    }
    Writer.writeString(builder.toString());
  }

  private static String toArticleListingEntry(Article article) {
    return "  " + article.getName();
  }

}
