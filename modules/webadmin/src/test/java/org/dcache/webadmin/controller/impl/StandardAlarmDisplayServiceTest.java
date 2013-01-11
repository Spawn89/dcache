/*
COPYRIGHT STATUS:
Dec 1st 2001, Fermi National Accelerator Laboratory (FNAL) documents and
software are sponsored by the U.S. Department of Energy under Contract No.
DE-AC02-76CH03000. Therefore, the U.S. Government retains a  world-wide
non-exclusive, royalty-free license to publish or reproduce these documents
and software for U.S. Government purposes.  All documents and software
available from this server are protected under the U.S. and Foreign
Copyright Laws, and FNAL reserves all rights.

Distribution of the software available from this server is free of
charge subject to the user following the terms of the Fermitools
Software Legal Information.

Redistribution and/or modification of the software shall be accompanied
by the Fermitools Software Legal Information  (including the copyright
notice).

The user is asked to feed back problems, benefits, and/or suggestions
about the software to the Fermilab Software Providers.

Neither the name of Fermilab, the  URA, nor the names of the contributors
may be used to endorse or promote products derived from this software
without specific prior written permission.

DISCLAIMER OF LIABILITY (BSD):

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED  WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED  WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL FERMILAB,
OR THE URA, OR THE U.S. DEPARTMENT of ENERGY, OR CONTRIBUTORS BE LIABLE
FOR  ANY  DIRECT, INDIRECT,  INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
OF SUBSTITUTE  GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY  OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT  OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE  POSSIBILITY OF SUCH DAMAGE.

Liabilities of the Government:

This software is provided by URA, independent from its Prime Contract
with the U.S. Department of Energy. URA is acting independently from
the Government and in its own private capacity and is not acting on
behalf of the U.S. Government, nor as its contractor nor its agent.
Correspondingly, it is understood and agreed that the U.S. Government
has no connection to this software and in no manner whatsoever shall
be liable for nor assume any responsibility or obligation for any claim,
cost, or damages arising out of or resulting from the use of the software
available from this server.

Export Control:

All documents and software available from this server are subject to U.S.
export control laws.  Anyone downloading information from this server is
obligated to secure any necessary Government licenses before exporting
documents or software obtained from this server.
 */
package org.dcache.webadmin.controller.impl;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.inOrder;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.dcache.alarms.dao.AlarmEntry;
import org.dcache.webadmin.controller.util.AlarmTableProvider;
import org.dcache.webadmin.model.dataaccess.IAlarmDAO;
import org.dcache.webadmin.model.dataaccess.impl.DAOFactoryImplHelper;
import org.dcache.webadmin.model.exceptions.DAOException;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

/**
 * Tests both the service refresh and the provider filtering and sorting.
 *
 * @author arossi
 */
public class StandardAlarmDisplayServiceTest {

    private IAlarmDAO mocked;
    private StandardAlarmDisplayService service;
    private AlarmTableProvider provider;
    private DAOFactoryImplHelper helper;

    @Before
    public void setup() throws Exception {
        helper = new DAOFactoryImplHelper();
        mocked = helper.getAlarmDAO();
        service = new StandardAlarmDisplayService(helper);
        provider = service.getDataProvider();
    }

    @Test
    public void shouldCallUpdateDeleteAndGetInOrderOnRefresh()
                    throws JSONException, DAOException {
        InOrder inOrder = inOrder(mocked);
        Set<AlarmEntry> update = givenSetOfAlarmEntriesOfLength(2);
        Set<AlarmEntry> delete = givenSetOfAlarmEntriesOfLength(3);
        givenEntriesAreSelectedForUpdateInProvider(update);
        givenEntriesAreSelectedForDeleteInProvider(delete);
        givenRefreshIsCalled();
        /*
         * the provider's lists are cleared internally; seems what mockito is
         * verifying is not the historical call but the current state!
         */
        update.clear();
        delete.clear();
        inOrder.verify(mocked).update(update);
        inOrder.verify(mocked).remove(delete);
        inOrder.verify(mocked).get(null, null, null, null);
    }

    @Test
    public void shouldFilterOnExpression() throws JSONException {
        int numberOfEntries = 4;
        Set<AlarmEntry> entries = givenSetOfAlarmEntriesOfLength(numberOfEntries);
        givenThatProviderAlarmsAre(entries);
        givenThatFilterExpressionIs("ALARM_2");
        Iterator<? extends AlarmEntry> it = provider.iterator(0,
                        numberOfEntries);
        int count = 0;
        AlarmEntry found = new AlarmEntry();
        while (it.hasNext()) {
            found = it.next();
            count++;
        }
        assertThat(count, is(1));
        assertThat(found.getType().equals("ALARM_2"), is(true));

        givenThatFilterExpressionIs("FOO");
        it = provider.iterator(0, numberOfEntries);
        count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertThat(count, is(0));
    }

    @Test
    public void shouldFilterOnNotShowClosed() throws JSONException {
        int numberOfEntries = 4;
        Set<AlarmEntry> entries = givenSetOfAlarmEntriesOfLength(numberOfEntries);
        AlarmEntry closed = givenThatEntryIsClosed(entries, 2);
        givenThatProviderAlarmsAre(entries);
        givenThatShowClosedIs(false);
        Iterator<? extends AlarmEntry> it = provider.iterator(0,
                        numberOfEntries);
        int count = 0;
        while (it.hasNext()) {
            assertThat(closed.equals(it.next()), is(false));
            count++;
        }
        assertThat(count, is(numberOfEntries - 1));
    }

    @Test
    public void shouldFilterOnRegex() throws JSONException {
        int numberOfEntries = 4;
        Set<AlarmEntry> entries = givenSetOfAlarmEntriesOfLength(numberOfEntries);
        givenThatProviderAlarmsAre(entries);
        givenThatRegularExpressionIs(".*LARM_0.*");
        Iterator<? extends AlarmEntry> it = provider.iterator(0,
                        numberOfEntries);
        int count = 0;
        AlarmEntry found = new AlarmEntry();
        while (it.hasNext()) {
            found = it.next();
            count++;
        }
        assertThat(count, is(1));
        assertThat(found.getType().equals("ALARM_0"), is(true));

        givenThatRegularExpressionIs(".*FOO.*");
        it = provider.iterator(0, numberOfEntries);
        count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertThat(count, is(0));
    }

    @Test
    public void shouldNotFilterOnShowClosed() throws JSONException {
        int numberOfEntries = 4;
        Set<AlarmEntry> entries = givenSetOfAlarmEntriesOfLength(numberOfEntries);
        givenThatEntryIsClosed(entries, 2);
        givenThatProviderAlarmsAre(entries);
        givenThatShowClosedIs(true);
        Iterator<? extends AlarmEntry> it = provider.iterator(0,
                        numberOfEntries);
        int count = 0;
        while (it.hasNext()) {
            it.next();
            count++;
        }
        assertThat(count, is(numberOfEntries));
    }

    @Test
    public void shouldSortOnCount() throws JSONException {
        int numberOfEntries = 20;
        Set<AlarmEntry> entries = givenSetOfAlarmEntriesOfLength(numberOfEntries);
        givenThatProviderAlarmsAre(entries);
        givenThatProviderShouldSortOnCount();
        Iterator<? extends AlarmEntry> it = provider.iterator(0,
                        numberOfEntries);
        int lastCount = 0;
        while (it.hasNext()) {
            int currentCount = it.next().getCount();
            assertThat(lastCount, lessThanOrEqualTo(currentCount));
            lastCount = currentCount;
        }
    }

    private void givenEntriesAreSelectedForDeleteInProvider(Set<AlarmEntry> list) {
        for (AlarmEntry entry : list) {
            provider.addToDeleted(entry);
        }
    }

    private void givenEntriesAreSelectedForUpdateInProvider(Set<AlarmEntry> list) {
        for (AlarmEntry entry : list) {
            provider.addToUpdated(entry);
        }
    }

    private void givenRefreshIsCalled() {
        service.refresh();
    }

    private Set<AlarmEntry> givenSetOfAlarmEntriesOfLength(int n)
                    throws JSONException {
        Set<AlarmEntry> set = new TreeSet<AlarmEntry>();
        for (int i = 0; i < n; i++) {
            AlarmEntry entry = new AlarmEntry();
            entry.setKey(UUID.randomUUID().toString());
            entry.setTimestamp(System.currentTimeMillis()
                            + TimeUnit.MINUTES.toMillis(i));
            entry.setSeverity(2);
            entry.setType("ALARM_" + i);
            entry.setCount(n - i);
            set.add(entry);
        }
        return set;
    }

    private AlarmEntry givenThatEntryIsClosed(Set<AlarmEntry> entries, int k) {
        int i = 0;
        for (AlarmEntry entry : entries) {
            if (i == k) {
                entry.setClosed(true);
                return entry;
            }
            i++;
        }
        return null;
    }

    private void givenThatFilterExpressionIs(String exp) {
        provider.setExpression(exp);
        provider.setRegex(false);
    }

    private void givenThatProviderAlarmsAre(Set<AlarmEntry> entries) {
        provider.setEntries(entries);
    }

    private void givenThatProviderShouldSortOnCount() {
        provider.setSort("count", SortOrder.ASCENDING);
    }

    private void givenThatRegularExpressionIs(String exp) {
        provider.setExpression(exp);
        provider.setRegex(true);
    }

    private void givenThatShowClosedIs(boolean showClosed) {
        provider.setShowClosed(showClosed);
    }
}