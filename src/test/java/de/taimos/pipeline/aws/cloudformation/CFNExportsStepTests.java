package de.taimos.pipeline.aws.cloudformation;

import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.amazonaws.client.builder.AwsSyncClientBuilder;
import com.amazonaws.services.cloudformation.AmazonCloudFormation;
import com.amazonaws.services.cloudformation.model.Export;
import com.amazonaws.services.cloudformation.model.ListExportsRequest;
import com.amazonaws.services.cloudformation.model.ListExportsResult;

import de.taimos.pipeline.aws.AWSClientFactory;
import hudson.model.Run;

@RunWith(PowerMockRunner.class)
@PrepareForTest(AWSClientFactory.class)
@PowerMockIgnore("javax.crypto.*")
public class CFNExportsStepTests {

	@Rule
	private JenkinsRule jenkinsRule = new JenkinsRule();
	private AmazonCloudFormation cloudFormation;

	@Before
	public void setupSdk() throws Exception {
		PowerMockito.mockStatic(AWSClientFactory.class);
		this.cloudFormation = Mockito.mock(AmazonCloudFormation.class);
		PowerMockito.when(AWSClientFactory.create(Mockito.any(AwsSyncClientBuilder.class), Mockito.any(StepContext.class)))
				.thenReturn(this.cloudFormation);
	}

	@Test
	public void listExports() throws Exception {
		WorkflowJob job = this.jenkinsRule.jenkins.createProject(WorkflowJob.class, "cfnTest");
		Mockito.when(this.cloudFormation.listExports(new ListExportsRequest()))
				.thenReturn(new ListExportsResult()
									.withNextToken("foo1")
									.withExports(new Export()
														 .withName("foo")
														 .withValue("bar")
									)
				);
		Mockito.when(this.cloudFormation.listExports(new ListExportsRequest().withNextToken("foo1")))
				.thenReturn(new ListExportsResult()
									.withExports(new Export()
														 .withName("baz")
														 .withValue("foo")
									)
				);
		job.setDefinition(new CpsFlowDefinition(""
														+ "node {\n"
														+ "  def exports = cfnExports()\n"
														+ "  echo \"exportsCount=${exports.size()}\"\n"
														+ "  echo \"foo=${exports['foo']}\"\n"
														+ "  echo \"baz=${exports['baz']}\"\n"
														+ "}\n", true)
		);

		Run run = this.jenkinsRule.assertBuildStatusSuccess(job.scheduleBuild2(0));
		this.jenkinsRule.assertLogContains("exportsCount=2", run);
		this.jenkinsRule.assertLogContains("foo=bar", run);
		this.jenkinsRule.assertLogContains("baz=foo", run);
	}

}
