/**
 * Copyright 2013 Apigee Corporation.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package io.apigee.trireme.core.internal;

import io.apigee.trireme.core.NodeEnvironment;
import org.mozilla.javascript.ClassShutter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.JavaScriptException;

import java.util.HashSet;

public class RhinoContextFactory
    extends ContextFactory
{
    private static final int DEFAULT_INSTRUCTION_THRESHOLD = 100000;

    private int jsVersion = NodeEnvironment.DEFAULT_JS_VERSION;
    private int optLevel = NodeEnvironment.DEFAULT_OPT_LEVEL;
    private boolean countOperations;

    @Override
    protected Context makeContext()
    {
        Context c = super.makeContext();
        c.setLanguageVersion(jsVersion);
        c.setOptimizationLevel(optLevel);
        c.setGenerateObserverCount(countOperations);
        if (countOperations) {
            c.setInstructionObserverThreshold(DEFAULT_INSTRUCTION_THRESHOLD);
        }
        c.setClassShutter(OpaqueClassShutter.INSTANCE);
        return c;
    }

    /**
     * Rhino will call this every "instruction observer threshold" bytecode instructions. We will look
     * on the current thread stack and if the expiration time is set, then we will
     */
    @Override
    protected void observeInstructionCount(Context cx, int count)
    {
        Object timeoutObj = cx.getThreadLocal(ScriptRunner.TIMEOUT_TIMESTAMP_KEY);
        if (timeoutObj == null) {
            return;
        }

        if (System.currentTimeMillis() > (Long)timeoutObj) {
            throw new JavaScriptException("Script timed out");
        }
    }

    public int getJsVersion()
    {
        return jsVersion;
    }

    public void setJsVersion(int jsVersion)
    {
        this.jsVersion = jsVersion;
    }

    public int getOptLevel()
    {
        return optLevel;
    }

    public void setOptLevel(int optLevel)
    {
        this.optLevel = optLevel;
    }

    public boolean isCountOperations()
    {
        return countOperations;
    }

    public void setCountOperations(boolean countOperations)
    {
        this.countOperations = countOperations;
    }

    /**
     * Don't allow access to Java code at all from inside Node code. However, Rhino seems to depend on access
     * to certain internal classes, at least for error handing, so we will allow the code to have access
     * to them.
     */
    private static final class OpaqueClassShutter
        implements ClassShutter
    {
        static final OpaqueClassShutter INSTANCE = new OpaqueClassShutter();

        private final HashSet<String> whitelist = new HashSet<String>();

        private OpaqueClassShutter()
        {
            whitelist.add("org.mozilla.javascript.EcmaError");
            whitelist.add("org.mozilla.javascript.EvaluatorException");
            whitelist.add("org.mozilla.javascript.JavaScriptException");
            whitelist.add("org.mozilla.javascript.RhinoException");
            whitelist.add("java.lang.Byte");
            whitelist.add("java.lang.Character");
            whitelist.add("java.lang.Class");
            whitelist.add("java.lang.Double");
            whitelist.add("java.lang.Exception");
            whitelist.add("java.lang.Float");
            whitelist.add("java.lang.Integer");
            whitelist.add("java.lang.Package");
            whitelist.add("java.lang.Long");
            whitelist.add("java.lang.Object");
            whitelist.add("java.lang.Short");
            whitelist.add("java.lang.Number");
            whitelist.add("java.lang.String");
            whitelist.add("java.lang.Throwable");
            whitelist.add("java.lang.Void");
        }

        @Override
        public boolean visibleToScripts(String s)
        {
            return (whitelist.contains(s));
        }
    }
}
