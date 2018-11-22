/**
 * This file is part of alf.io.
 *
 * alf.io is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * alf.io is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with alf.io.  If not, see <http://www.gnu.org/licenses/>.
 */
package alfio.manager;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class Jobs {

    private static final int THIRTY_SECONDS = 1000 * 30;

    private final WaitingQueueSubscriptionProcessor waitingQueueSubscriptionProcessor;

    void processReleasedTickets() {
        waitingQueueSubscriptionProcessor.handleWaitingTickets();
    }

    @DisallowConcurrentExecution
    @Log4j2
    public static class ProcessReleasedTickets implements Job {

        public static long INTERVAL = THIRTY_SECONDS;

        @Autowired
        private Jobs jobs;

        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException {
            log.trace("running job " + getClass().getSimpleName());
            jobs.processReleasedTickets();
        }
    }
}
