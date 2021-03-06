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

import jef.database.jsqlparser.expression.Function;
import jef.database.jsqlparser.expression.Table;
import jef.database.jsqlparser.visitor.DeParserAdapter;
import jef.database.jsqlparser.visitor.Expression;
import jef.database.jsqlparser.visitor.SelectItem;
import jef.database.jsqlparser.visitor.SelectItemVisitor;

/**
 * An expression as in "SELECT expr1 AS EXPR"
 */
public class SelectExpressionItem implements SelectItem {

	private Expression expression;

	private String alias;

	public SelectExpressionItem(){
	}
	
	public SelectExpressionItem(Expression expression,String alias){
		this.expression=expression;
		this.alias=alias;
	}
	
	public String getAlias() {
		return alias;
	}

	public Expression getExpression() {
		return expression;
	}

	public void setAlias(String string) {
		alias = string;
	}

	public void setExpression(Expression expression) {
		this.expression = expression;
	}

	public void accept(SelectItemVisitor selectItemVisitor) {
		selectItemVisitor.visit(this);
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		appendTo(sb);
		return sb.toString();
	}

	public void appendTo(StringBuilder sb) {
		expression.appendTo(sb);
		if (alias != null) {
			sb.append(" AS ").append(alias);
		}
	}

	

	public SelectExpressionItem getAsSelectExpression() {
		return this;
	}

	public Table getTableOfAllColumns() {
		throw new IllegalStateException();
	}

	public boolean isAllColumns() {
		return false;
	}

	public String getStringWithoutGroupFuncAndAlias() {
		DeParserAdapter dep = new DeParserAdapter() {
			@Override
			public void visit(Function function) {
				if (function.getGroupFunctionType() != null) {
					if (function.getParamCount() == 1) {
						function.getParameters().getExpressions().get(0).accept(this);
						return;
					} else if (function.isAllColumns()) {
						sb.append("*");
						return;
					}
				}
				super.visit(function);
			}
		};
		this.expression.accept(dep);
		return dep.toString();
	}
}
