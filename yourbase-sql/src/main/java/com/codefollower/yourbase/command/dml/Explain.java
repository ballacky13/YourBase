/*
 * Copyright 2004-2011 H2 Group. Multiple-Licensed under the H2 License,
 * Version 1.0, and under the Eclipse Public License, Version 1.0
 * (http://h2database.com/html/license.html).
 * Initial Developer: H2 Group
 */
package com.codefollower.yourbase.command.dml;

import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import com.codefollower.yourbase.command.CommandInterface;
import com.codefollower.yourbase.command.Prepared;
import com.codefollower.yourbase.dbobject.table.Column;
import com.codefollower.yourbase.engine.Database;
import com.codefollower.yourbase.engine.Session;
import com.codefollower.yourbase.expression.Expression;
import com.codefollower.yourbase.expression.ExpressionColumn;
import com.codefollower.yourbase.result.LocalResult;
import com.codefollower.yourbase.result.ResultInterface;
import com.codefollower.yourbase.value.Value;
import com.codefollower.yourbase.value.ValueString;

/**
 * This class represents the statement
 * EXPLAIN
 */
public class Explain extends Prepared {

    private Prepared command;
    private LocalResult result;
    private boolean executeCommand;

    public Explain(Session session) {
        super(session);
    }

    public void setCommand(Prepared command) {
        this.command = command;
    }

    public void prepare() {
        command.prepare();
    }

    public void setExecuteCommand(boolean executeCommand) {
        this.executeCommand = executeCommand;
    }

    public ResultInterface queryMeta() {
        return query(-1);
    }

    public ResultInterface query(int maxrows) {
        Column column = new Column("PLAN", Value.STRING);
        Database db = session.getDatabase();
        ExpressionColumn expr = new ExpressionColumn(db, column);
        Expression[] expressions = { expr };
        result = new LocalResult(session, expressions, 1);
        if (maxrows >= 0) {
            String plan;
            if (executeCommand) {
                db.statisticsStart();
                if (command.isQuery()) {
                    command.query(maxrows);
                } else {
                    command.update();
                }
                plan = command.getPlanSQL();
                Map<String, Integer> statistics = db.statisticsEnd();
                if (statistics != null) {
                    int total = 0;
                    for (Entry<String, Integer> e : statistics.entrySet()) {
                        total += e.getValue();
                    }
                    if (total > 0) {
                        statistics = new TreeMap<String, Integer>(statistics);
                        StringBuilder buff = new StringBuilder();
                        buff.append("total: ").append(total).append('\n');
                        for (Entry<String, Integer> e : statistics.entrySet()) {
                            int value = e.getValue();
                            int percent = (int) (100L * value / total);
                            buff.append(e.getKey()).append(": ").append(value).append(" (").append(percent).append("%)\n");
                        }
                        plan += "\n/*\n" + buff.toString() + "*/";
                    }
                }
            } else {
                plan = command.getPlanSQL();
            }
            add(plan);
        }
        result.done();
        return result;
    }

    private void add(String text) {
        Value[] row = { ValueString.get(text) };
        result.addRow(row);
    }

    public boolean isQuery() {
        return true;
    }

    public boolean isTransactional() {
        return true;
    }

    public boolean isReadOnly() {
        return command.isReadOnly();
    }

    public int getType() {
        return CommandInterface.EXPLAIN;
    }
}
