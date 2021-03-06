package org.jbpm.more;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.drools.core.command.impl.GenericCommand;
import org.drools.core.command.impl.KnowledgeCommandContext;
import org.drools.core.common.InternalKnowledgeRuntime;
import org.jbpm.process.instance.InternalProcessRuntime;
import org.jbpm.process.instance.timer.TimerInstance;
import org.jbpm.process.instance.timer.TimerManager;
import org.jbpm.ruleflow.instance.RuleFlowProcessInstance;
import org.jbpm.workflow.instance.node.HumanTaskNodeInstance;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.NodeInstance;

public class UpgradeCommand implements GenericCommand<Void> {
	private static final long serialVersionUID = 1L;

	private final long processInstanceId;
	private final long newDelay;

	public UpgradeCommand(long processInstanceId, long newDelayInMs) {
		this.processInstanceId = processInstanceId;
		this.newDelay = newDelayInMs;
	}

	public Void execute(org.kie.internal.command.Context context) {
		KieSession ksession = ((KnowledgeCommandContext) context).getKieSession();
		RuleFlowProcessInstance wfp = (RuleFlowProcessInstance)
			ksession.getProcessInstance(processInstanceId);
		HumanTaskNodeInstance taskNodeInstance = null;
		for (NodeInstance nodeInstance: wfp.getNodeInstances()) {
			if (nodeInstance instanceof HumanTaskNodeInstance) {
				taskNodeInstance = (HumanTaskNodeInstance) nodeInstance;
			}
		}
		TimerManager timerManager = ((InternalProcessRuntime) ((InternalKnowledgeRuntime)
			ksession).getProcessRuntime()).getTimerManager();
		List<Long> newTimerInstances = new ArrayList<Long>();
		for (long timerInstanceId: taskNodeInstance.getTimerInstances()) {
			System.out.println("Found timer " + timerInstanceId);
			TimerInstance oldTimerInstance = timerManager.getTimerMap().get(timerInstanceId);
			timerManager.cancelTimer(timerInstanceId);
			TimerInstance newTimerInstance = new TimerInstance();
			long timePassed = new Date().getTime() - oldTimerInstance.getActivated().getTime();
			// wait 25 second (minus time already passed)
			newTimerInstance.setDelay(newDelay - timePassed);
			System.out.println("Setting timer delay to " + newTimerInstance.getDelay() + " ms");
			newTimerInstance.setPeriod(0);
			newTimerInstance.setTimerId(oldTimerInstance.getTimerId());
			timerManager.registerTimer(newTimerInstance, wfp);
			newTimerInstances.add(newTimerInstance.getId());
		}
		taskNodeInstance.internalSetTimerInstances(newTimerInstances);
		return null;
	}
}