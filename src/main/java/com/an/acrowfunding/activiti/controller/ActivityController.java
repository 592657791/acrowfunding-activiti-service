package com.an.acrowfunding.activiti.controller;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.pvm.process.ProcessDefinitionImpl;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.apache.tomcat.util.IntrospectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.an.acrowfunding.common.bean.Member;
import com.an.acrowfunding.common.bean.ResultAjax;

@RestController
public class ActivityController {
	
	@Autowired
	RepositoryService repositoryService;
	
	@Autowired
	RuntimeService runtimeService;
	
	@Autowired
	TaskService taskService;
	
	@RequestMapping("/act/completeTask")
	public void completeTask(@RequestBody Map<String, Object> variables) {
		System.out.println("Map:"+variables.get("piid"));
		
		TaskQuery query = taskService.createTaskQuery();
		
		Task task = query.processInstanceId((String)variables.get("piid"))
			.taskAssignee((String)variables.get("loginacct")+"")
			.singleResult();
		taskService.complete(task.getId(),variables);
	}
	
	
	@RequestMapping("/act/startProcess")
	public String startProcess(@RequestBody Member loginMember) {
		ProcessDefinition pd = repositoryService.createProcessDefinitionQuery()
						  .processDefinitionKey("authflow")
						  .latestVersion()
						  .singleResult();
		Map<String, Object> variables = new HashMap<>();
		variables.put("loginacct", loginMember.getLoginacct());
		
		ProcessInstance pi = runtimeService.startProcessInstanceById(pd.getId(), variables);
		
		return pi.getId();
	}
	
	
	@RequestMapping("/act/queryDataList")
	public List<Map<String,Object>> queryDataList(@RequestBody Map<String, Object>paramMap) {
		
		//ResultAjax result = new ResultAjax();
		ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();
		
		int startindex = (int) paramMap.get("startindex");
		int pagesize = (int) paramMap.get("pagesize");
		
		List<ProcessDefinition> listPage = query.listPage(startindex, pagesize);
		
		List<Map<String, Object>> list = new ArrayList<>();
		
		for (ProcessDefinition processDefinition : listPage) {
			Map<String, Object> map = new HashMap<>();
			map.put("id", processDefinition.getId());
			map.put("name", processDefinition.getName());
			map.put("version", processDefinition.getVersion());
			map.put("key", processDefinition.getKey());
			map.put("deployid", processDefinition.getDeploymentId());
			list.add(map);
		}
		return list;
	}
	
	@RequestMapping("/act/queryPageCount")
	public Object queryPageCount() {
		
		ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();
		
		int count = (int) query.count();
		
		return count;
		
	}
	
	@RequestMapping("/act/upload")
	public ResultAjax upload(@RequestParam("pdfile")MultipartFile file) throws IOException {
		
		ResultAjax result = new ResultAjax();
		try {
			InputStream in = file.getInputStream();
			Deployment deploy = repositoryService.createDeployment().addInputStream(file.getOriginalFilename(), in).deploy();
			result.setSuccess(true);
		} catch (Exception e) {
			result.setSuccess(false);
			e.printStackTrace();
		}
		return result;
	}
	
	@RequestMapping("/act/loadImgById/{id}")
	public byte[] loadImgById(@PathVariable("id")String id) {
		// 部署ID ==>  流程定义ID
		// 从数据库中读取流程定义的图片
		//根据流程部署id和部署资源名称获取部署图片的输入流。
		System.out.println("Act Id:"+id);
		ProcessDefinitionQuery query = repositoryService.createProcessDefinitionQuery();
		ProcessDefinition pd = query.processDefinitionId(id).singleResult();
		InputStream in = repositoryService.getResourceAsStream(pd.getDeploymentId(), pd.getDiagramResourceName());
		
		ByteArrayOutputStream swapStream = new ByteArrayOutputStream(); 
		byte[] buff = new byte[100]; //buff用于存放循环读取的临时数据 
		int rc = 0; 
		try {
			while ((rc = in.read(buff, 0, 100)) > 0) { 
			    swapStream.write(buff, 0, rc); 
			}
		} catch (IOException e) {
			e.printStackTrace();
		} 
		byte[] in_b = swapStream.toByteArray(); //in_b为转换之后的结果
		
		return in_b;
	}
	
	@RequestMapping("/act/delete/{id}")
	public ResultAjax delete(@PathVariable("id")String id) {
		ResultAjax result = new ResultAjax();
		try {
			repositoryService.deleteDeployment(id, true);//部署流程Id,级联删除
			result.setSuccess(true);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			result.setSuccess(false);
			e.printStackTrace();
		}
		return result;
	}
}
