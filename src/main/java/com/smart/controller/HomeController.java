package com.smart.controller;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import javax.websocket.Session;

import org.apache.tomcat.util.net.AprEndpoint.Sendfile;
import org.aspectj.bridge.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.objenesis.instantiator.basic.NewInstanceInstantiator;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entities.User;

@Controller
public class HomeController {
	@Autowired
	private BCryptPasswordEncoder passwordEncoder;
	@Autowired
	public UserRepository userRepository;
	
	@RequestMapping("/")
	public String home(Model model) {
		model.addAttribute("title","Home - Smart Contact Manager");
		return "home";
	}
	
	@RequestMapping("/about")
	public String about(Model model) {
		model.addAttribute("title","About - Smart Contact Manager");
		return "about";
	}
	
	@RequestMapping("/signup")
	public String signup(Model model) {
		model.addAttribute("title","Register - Smart Contact Manager");
		model.addAttribute("user",new User());
		return "signup";
	}
	
//	handler for registering user
	@RequestMapping(value="/do_register",method = RequestMethod.POST)
	public String registerUser(@Valid @ModelAttribute("user") User user,BindingResult bindingResult,
			@RequestParam(value = "agreement",defaultValue = "false") boolean agreement, 
			Model model,HttpSession session) {
		 
		try {
			if (!agreement) {
				//System.out.println("You have not agreed to terms and conditions!");
				throw new Exception("You have not agreed the terms and conditions");
			}
			if(bindingResult.hasErrors()) {
				System.out.println("Error : "+bindingResult.toString());
				model.addAttribute("user",user);
				return "signup";
			}
			 
			user.setRole("ROLE_USER");
			user.setEnabled(true);
			user.setImageUrl("default.png");
			user.setPassword(passwordEncoder.encode(user.getPassword()));
			
			
			//System.out.println("User"+user);
			//System.out.println("Agreement : "+agreement);
			//save in the database
			User user2 = this.userRepository.save(user);
			//send values to next page
			model.addAttribute("user : ",user2);
			session.setAttribute("message", new com.smart.helper.Message("Successfully Registered !!", "alert-success")); 
			return "signup";
			
		} catch (Exception e) {
			e.printStackTrace();
			model.addAttribute("user",user);
			session.setAttribute("message", new com.smart.helper.Message("Something went wrong!!"+e.getMessage(), "alert-error")); 
			return "signup";
		}
	}
	
	//login handler
	@GetMapping("/signin")
	public String customLogin(Model model) {
		model.addAttribute("title","Login Page");
		return "login";
	}
	
}
