/**
 * Tigase XMPP Server - The instant messaging server
 * Copyright (C) 2004 Tigase, Inc. (office@tigase.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. Look for COPYING file in the top folder.
 * If not, see http://www.gnu.org/licenses/.
 */
package tigase.monitor;

import tigase.db.TigaseDBException;
import tigase.db.comp.ComponentRepository;
import tigase.db.comp.RepositoryChangeListenerIfc;
import tigase.form.Form;
import tigase.kernel.beans.Bean;
import tigase.kernel.beans.Inject;
import tigase.kernel.core.Kernel;
import tigase.monitor.TaskConfigItem.Type;
import tigase.monitor.tasks.ScriptTask;
import tigase.monitor.tasks.ScriptTimerTask;

import javax.script.ScriptException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Bean(name = TasksScriptRegistrar.ID, active = true)
public class TasksScriptRegistrar {

	public static final String ID = "TasksScriptRegistrar";
	protected final Logger log = Logger.getLogger(this.getClass().getName());
	@Inject
	private Kernel kernel;
	@Inject(nullAllowed = false)
	private ComponentRepository<TaskConfigItem> repo;

	public void delete(String taskName) {
		try {
			repo.removeItem(taskName);
		} catch (TigaseDBException e) {
			log.log(Level.WARNING, "Error accessing repository", e);
		}
	}

	public Kernel getKernel() {
		return kernel;
	}

	public void setKernel(Kernel kernel) {
		this.kernel = kernel;
	}

	public ComponentRepository<TaskConfigItem> getRepo() {
		return repo;
	}

	public void setRepo(ComponentRepository<TaskConfigItem> repo) {
		this.repo = repo;
		this.repo.addRepoChangeListener(new RepositoryChangeListenerIfc<TaskConfigItem>() {

			@Override
			public void itemAdded(TaskConfigItem item) {
				if (!kernel.isBeanClassRegistered(item.getTaskName())) {
					try {
						initTaskFromTaskConfig(item);
					} catch (Exception e) {
						log.log(Level.WARNING, "Problem during initializing task", e);
					}
				}
			}

			@Override
			public void itemRemoved(TaskConfigItem item) {
				try {
					kernel.unregister(item.getTaskName());
				} catch (Exception e) {
					log.log(Level.WARNING, "Problem during unregistering task", e);
				}
			}

			@Override
			public void itemUpdated(TaskConfigItem item) {
				try {
					MonitorTask task = kernel.getInstance(item.getTaskName());
					if (task instanceof ConfigurableTask) {
						((ConfigurableTask) task).setNewConfiguration(item.getConfiguration());
					}
				} catch (Exception e) {
					log.log(Level.WARNING, "Problem during configuring task", e);
				}
			}
		});
	}

	public void load() {
		try {
			for (TaskConfigItem item : repo.allItems()) {
				initTaskFromTaskConfig(item);
			}
		} catch (Exception e) {
			log.log(Level.WARNING, "Error loading task", e);
		}
	}

	public void registerScript(String scriptName, String scriptExtension, String scriptContent) throws ScriptException {
		ScriptTask task = runScriptTask(scriptName, scriptExtension, scriptContent);
		saveScript(Type.scriptTask, scriptName, scriptExtension, scriptContent, task.getCurrentConfiguration());
	}

	public void registerTimerScript(String scriptName, String scriptExtension, String scriptContent, Long delay)
			throws ScriptException {
		ScriptTimerTask task = runScriptTimerTask(scriptName, scriptExtension, scriptContent, delay);
		saveScript(Type.scriptTimerTask, scriptName, scriptExtension, scriptContent, task.getCurrentConfiguration());
	}

	public void updateConfig(String taskName, Form form) {
		MonitorTask task = kernel.getInstance(taskName);

		TaskConfigItem item = repo.getItem(taskName);
		if (item == null) {
			item = new TaskConfigItem();
			item.setTaskName(taskName);
			if (task instanceof ScriptTimerTask) {
				item.setType(Type.scriptTimerTask);
				item.setScriptExtension(((ScriptTimerTask) task).getScriptExtension());
				item.setTaskScript(((ScriptTimerTask) task).getScript());
			} else if (task instanceof ScriptTask) {
				item.setType(Type.scriptTask);
				item.setScriptExtension(((ScriptTask) task).getScriptExtension());
				item.setTaskScript(((ScriptTask) task).getScript());
			} else {
				item.setType(Type.task);
				item.setTaskClass(task.getClass());
			}
		}
		item.setConfiguration(form);

		try {
			repo.addItem(item);
		} catch (TigaseDBException e) {
			log.log(Level.WARNING, "Error accessing repository", e);
		}
	}

	private void initTaskFromTaskConfig(final TaskConfigItem item) throws ScriptException, TigaseDBException {
		Type type;
		try {
			type = item.getType();
			if (type == null) {
				repo.removeItem(item.getKey());
				return;
			}
		} catch (Exception e) {
			repo.removeItem(item.getKey());
			return;
		}
		String taskName = item.getTaskName();
		String scriptExtension = item.getScriptExtension();
		String scriptContent = item.getTaskScript();

		switch (type) {
			case task:
				if (kernel.isBeanClassRegistered(item.getTaskName())) {
					MonitorTask task = kernel.getInstance(item.getTaskName());
					if (task instanceof ConfigurableTask) {
						((ConfigurableTask) task).setNewConfiguration(item.getConfiguration());
					}
				} else {
					repo.removeItem(item.getKey());
				}
				break;
			case scriptTask:
				runScriptTask(taskName, scriptExtension, scriptContent, item.getConfiguration());
			case scriptTimerTask:
				runScriptTimerTask(taskName, scriptExtension, scriptContent, item.getConfiguration());
			default:
				break;
		}

	}

	private ScriptTask runScriptTask(String scriptName, String scriptExtension, String scriptContent)
			throws ScriptException {
		kernel.registerBean(scriptName).asClass(ScriptTask.class).exec();
		ScriptTask scriptTask = kernel.getInstance(scriptName);
		scriptTask.setScript(scriptContent, scriptExtension);
		scriptTask.setEnabled(true);
		return scriptTask;
	}

	private ScriptTask runScriptTask(String scriptName, String scriptExtension, String scriptContent, Form config)
			throws ScriptException {
		kernel.registerBean(scriptName).asClass(ScriptTask.class).exec();
		ScriptTask scriptTask = kernel.getInstance(scriptName);
		scriptTask.setScript(scriptContent, scriptExtension);
		scriptTask.setNewConfiguration(config);
		return scriptTask;
	}

	private ScriptTimerTask runScriptTimerTask(String scriptName, String scriptExtension, String scriptContent,
											   Form config) throws ScriptException {
		kernel.registerBean(scriptName).asClass(ScriptTimerTask.class).exec();
		ScriptTimerTask scriptTask = kernel.getInstance(scriptName);
		scriptTask.setScript(scriptContent, scriptExtension);
		scriptTask.setNewConfiguration(config);
		return scriptTask;
	}

	private ScriptTimerTask runScriptTimerTask(String scriptName, String scriptExtension, String scriptContent,
											   Long delay) throws ScriptException {
		kernel.registerBean(scriptName).asClass(ScriptTimerTask.class).exec();
		ScriptTimerTask scriptTask = kernel.getInstance(scriptName);
		scriptTask.setScript(scriptContent, scriptExtension);
		scriptTask.setPeriod(delay);
		scriptTask.setEnabled(true);
		return scriptTask;
	}

	private void saveScript(Type type, String scriptName, String scriptExtension, String scriptContent,
							Form configuration) {
		TaskConfigItem item = new TaskConfigItem();
		item.setTaskName(scriptName);
		item.setConfiguration(configuration);
		item.setScriptExtension(scriptExtension);
		item.setTaskScript(scriptContent);
		item.setType(type);

		try {
			repo.addItem(item);
		} catch (TigaseDBException e) {
			e.printStackTrace();
		}

	}

}
