/* 
* Copyright (C) 2003-2015 eXo Platform SAS.
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see http://www.gnu.org/licenses/ .
*/
package org.exoplatform.task.dao.jpa;

import org.exoplatform.task.dao.TaskHandler;
import org.exoplatform.task.dao.TaskQuery;
import org.exoplatform.task.domain.Project;
import org.exoplatform.task.domain.Task;
import org.exoplatform.task.service.jpa.TaskServiceJPAImpl;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : Thibault Clement
 * tclement@exoplatform.com
 * 4/8/15
 */
public class TaskDAOImpl extends GenericDAOImpl<Task, Long> implements TaskHandler {

  public TaskDAOImpl(TaskServiceJPAImpl taskServiceJPAImpl) {
    super(taskServiceJPAImpl);
  }

  @Override
  public List<Task> findByProject(Long projectId) {
    EntityManager em = taskService.getEntityManager();
    Query query = em.createNamedQuery("Task.findTaskByProject", Task.class);
    query.setParameter("projectId", projectId);
    return query.getResultList();
  }

  @Override
  public List<Task> findByUser(String user) {

    List<String> memberships = new ArrayList<String>();
    memberships.add(user);

    return  findAllByMembership(user, memberships);
  }

  @Override
  public List<Task> findAllByMembership(String user, List<String> memberships) {

    Query query = taskService.getEntityManager().createNamedQuery("Task.findByMemberships", Task.class);
    query.setParameter("userName", user);
    query.setParameter("memberships", memberships);

    return query.getResultList();
  }

  @Override
  public List<Task> findByTag(String tag) {
    return null;
  }

  @Override
  public List<Task> findByTags(List<String> tags) {
    return null;
  }

  @Override
  public List<Task> findTaskByQuery(TaskQuery query) {
    EntityManager em = taskService.getEntityManager();
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Task> q = cb.createQuery(Task.class);

    Root<Task> task = q.from(Task.class);
    q.select(task);

    List<Predicate> predicates = new ArrayList<Predicate>();

    if(query.getTaskId() > 0) {
      predicates.add(cb.equal(task.get("id"), query.getTaskId()));
    }

    if (query.getTitle() != null && !query.getTitle().isEmpty()) {
      predicates.add(cb.like(task.<String>get("title"), "%" + query.getTitle() + "%"));
    }

    if (query.getDescription() != null && !query.getDescription().isEmpty()) {
      predicates.add(cb.like(task.<String>get("description"), '%' + query.getDescription() + '%'));
    }

    if (query.getAssignee() != null && !query.getAssignee().isEmpty()) {
      predicates.add(cb.like(task.<String>get("assignee"), '%' + query.getAssignee() + '%'));
    }

    if(query.getProjectId() > 0) {
      Join<Task, Project> project = task.join("project");
      predicates.add(cb.equal(project.get("id"), query.getProjectId()));
    }

    if(query.getKeyword() != null && !query.getKeyword().isEmpty()) {
      String keyword = "%" + query.getKeyword() + "%";
      predicates.add(
              cb.or(
                  cb.like(task.<String>get("title"), keyword),
                  cb.like(task.<String>get("description"), keyword),
                  cb.like(task.<String>get("assignee"), keyword)
              )
      );
    }

    if(predicates.size() > 0) {
      Iterator<Predicate> it = predicates.iterator();
      Predicate p = it.next();
      while(it.hasNext()) {
        p = cb.and(p, it.next());
      }
      q.where(p);
    }

    return em.createQuery(q).getResultList();
  }
}
