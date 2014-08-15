/*
 * JEF - Copyright 2009-2010 Jiyi (mr.jiyi@gmail.com)
 *
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
 */
package jef.database.jsqlparser.statement.select;

import jef.database.jsqlparser.expression.Table;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.SelectItem;
import jef.database.jsqlparser.visitor.SelectItemVisitor;

public class AllTableColumns implements SelectItem {

    private Table table;

    public AllTableColumns() {
    }

    public AllTableColumns(Table tableName) {
        this.table = tableName;
    }

    public Table getTable() {
        return table;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public void accept(SelectItemVisitor selectItemVisitor) {
        selectItemVisitor.visit(this);
    }

    public String toString() {
        return table + ".*";
    }

	public void appendTo(StringBuilder sb) {
		sb.append(table).append(".*");
	}

	public Expression getExpression() {
		return null;
	}

	public String getAlias() {
		return null;
	}

	public void appendTo(StringBuilder sb, boolean noGroupFunc) {
		appendTo(sb);
	}
}
