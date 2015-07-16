/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.transaction;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NoInitialContextException;
import javax.transaction.*;
import java.util.Hashtable;

@RunWith(Arquillian.class)
@RunAsClient
public class TransactionSuspendTest {
    @ArquillianResource
    private ManagementClient managementClient;

    private static Context context;

    @BeforeClass
    public static void beforeClass() throws Exception {
        final Hashtable props = new Hashtable();
        //props.put(Context.INITIAL_CONTEXT_FACTORY, "org.jnp.interfaces.NamingContextFactory");
        //props.put(Context.URL_PKG_PREFIXES, "org.jboss.naming:org.jnp.interfaces");
        props.put(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");
        context = new InitialContext(props);
    }

    @Deployment
    public static WebArchive deployment() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "transaction.war");
        war.addPackage(TransactionManagerSuspendTest.class.getPackage());
        return war;
    }

    @Test
    public void testNothing() {
        System.out.printf("TODO");
    }

    // TODO fails because RunAsClient means jndi lookups fail    @Test
    public void testTMWhileSuspended() throws Exception {
        TransactionManager tm = (TransactionManager)new InitialContext().lookup("java:jboss/TransactionManager");
        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP).set("suspend");
        managementClient.getControllerClient().execute(op);

        try {
            // start transaction
            tm.begin();
            Assert.fail("begin txn via TM should have been rejected");
        } catch (IllegalStateException expected) {

        } finally {
            op = new ModelNode();
            op.get(ModelDescriptionConstants.OP).set("resume");
            managementClient.getControllerClient().execute(op);
        }
    }

    // TODO fails because RunAsClient means jndi lookups fail    @Test
    public void testUTWhileSuspended() throws Exception {
        ModelNode op = new ModelNode();
        op.get(ModelDescriptionConstants.OP).set("suspend");
        managementClient.getControllerClient().execute(op);

        try {
            // start transaction
            final UserTransaction transaction = (UserTransaction) new InitialContext().lookup("java:comp/UserTransaction");
            transaction.begin();
            Assert.fail("begin txn via UT should have been rejected");
        } catch (NoInitialContextException unexpected) {
            Assert.fail(unexpected.getMessage());
        } catch (IllegalStateException expected) {

        } finally {
            op = new ModelNode();
            op.get(ModelDescriptionConstants.OP).set("resume");
            managementClient.getControllerClient().execute(op);
        }
    }
}
