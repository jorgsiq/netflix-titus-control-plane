/*
 * Copyright 2018 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.titus.master.integration.v3.agent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.netflix.titus.api.jobmanager.model.job.JobDescriptor;
import com.netflix.titus.api.jobmanager.model.job.JobFunctions;
import com.netflix.titus.api.jobmanager.model.job.JobGroupInfo;
import com.netflix.titus.api.jobmanager.model.job.Task;
import com.netflix.titus.api.jobmanager.model.job.ext.ServiceJobExt;
import com.netflix.titus.common.aws.AwsInstanceType;
import com.netflix.titus.master.integration.BaseIntegrationTest;
import com.netflix.titus.master.integration.v3.scenario.InstanceGroupScenarioTemplates;
import com.netflix.titus.master.integration.v3.scenario.InstanceGroupsScenarioBuilder;
import com.netflix.titus.master.integration.v3.scenario.JobsScenarioBuilder;
import com.netflix.titus.master.integration.v3.scenario.ScenarioTemplates;
import com.netflix.titus.testkit.embedded.cell.EmbeddedTitusCell;
import com.netflix.titus.testkit.embedded.cell.master.EmbeddedTitusMasters;
import com.netflix.titus.testkit.embedded.cloud.SimulatedCloud;
import com.netflix.titus.testkit.embedded.cloud.model.SimulatedAgentGroupDescriptor;
import com.netflix.titus.testkit.junit.category.IntegrationTest;
import com.netflix.titus.testkit.junit.master.TitusStackResource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;

import static com.netflix.titus.api.jobmanager.model.job.JobFunctions.changeSecurityGroups;
import static com.netflix.titus.testkit.model.job.JobDescriptorGenerator.oneTaskServiceJobDescriptor;
import static org.assertj.core.api.Assertions.assertThat;

@Category(IntegrationTest.class)
public class ResourceSchedulingTest extends BaseIntegrationTest {

    private final TitusStackResource titusStackResource = new TitusStackResource(EmbeddedTitusCell.aTitusCell()
            .withMaster(
                    EmbeddedTitusMasters.basicMaster(
                            new SimulatedCloud().createAgentInstanceGroups(
                                    new SimulatedAgentGroupDescriptor("flex1", AwsInstanceType.M5_Metal.name(), 0, 1, 1, 2)
                            ))
            )
            .withDefaultGateway()
            .build()
    );

    private final JobsScenarioBuilder jobsScenarioBuilder = new JobsScenarioBuilder(titusStackResource);

    private final InstanceGroupsScenarioBuilder instanceGroupsScenarioBuilder = new InstanceGroupsScenarioBuilder(titusStackResource);

    @Rule
    public RuleChain ruleChain = RuleChain.outerRule(titusStackResource).around(instanceGroupsScenarioBuilder).around(jobsScenarioBuilder);

    @Before
    public void setUp() throws Exception {
        instanceGroupsScenarioBuilder.synchronizeWithCloud().template(InstanceGroupScenarioTemplates.activate("flex1"));
    }

    /**
     * Verify ENI assignment
     */
    @Test(timeout = 30_000)
    public void checkIpPerEniLimitIsPreserved() throws Exception {
        JobDescriptor<ServiceJobExt> jobDescriptor = JobFunctions.changeServiceJobCapacity(oneTaskServiceJobDescriptor(), 3);

        jobsScenarioBuilder.schedule(jobDescriptor, jobScenarioBuilder -> jobScenarioBuilder
                .template(ScenarioTemplates.startTasksInNewJob())
                .assertTasks(tasks -> {
                    Set<Integer> indexes = new HashSet<>();
                    for (Task task : tasks) {
                        indexes.add(task.getTwoLevelResources().get(0).getIndex());
                    }
                    return indexes.size() == 2;
                })
        );
    }

    /**
     * Verify same security groups with different list orders are scheduled to same ENI index.
     */
    @Test(timeout = 30_000)
    public void verifyDifferentSecurityGroupOrderingReusesEni() throws Exception {
        JobDescriptor<ServiceJobExt> firstJobDescriptor = changeSecurityGroups(oneTaskServiceJobDescriptor(),
                Arrays.asList("sg-1", "sg-2", "sg-3")).toBuilder().withJobGroupInfo(JobGroupInfo.newBuilder().build()).build();
        JobDescriptor<ServiceJobExt> secondJobDescriptor = changeSecurityGroups(oneTaskServiceJobDescriptor(),
                Arrays.asList("sg-2", "sg-3", "sg-1")).toBuilder().withJobGroupInfo(JobGroupInfo.newBuilder().build()).build();

        jobsScenarioBuilder.schedule(firstJobDescriptor, jobScenarioBuilder -> jobScenarioBuilder
                .template(ScenarioTemplates.startTasksInNewJob())
        );
        jobsScenarioBuilder.schedule(secondJobDescriptor, jobScenarioBuilder -> jobScenarioBuilder
                .template(ScenarioTemplates.startTasksInNewJob())
        );

        int firstEniIndex = jobsScenarioBuilder.takeJob(0).getTaskByIndex(0).getTask().getTwoLevelResources().get(0).getIndex();
        int secondEniIndex = jobsScenarioBuilder.takeJob(1).getTaskByIndex(0).getTask().getTwoLevelResources().get(0).getIndex();
        assertThat(firstEniIndex).isEqualTo(secondEniIndex);
    }
}