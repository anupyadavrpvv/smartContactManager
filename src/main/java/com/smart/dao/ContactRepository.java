package com.smart.dao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smart.entities.Contacts;

public interface ContactRepository extends JpaRepository<Contacts, Integer> {
	//here in hql use name of the bean i.e. Contacts not the database table name i.e. Contact
	//pageable object will return current page and contact per page this is called pagination
	@Query(value = "select c from Contacts as c where c.user.id =:userId")
	public Page<Contacts> findContactsByUserId(@Param("userId") int userId, Pageable pageable); 
}
