package org.trophic.graph.controller;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.trophic.graph.domain.User;
import org.trophic.graph.service.CineastsRepository;
import org.trophic.graph.service.CineastsUserDetailsService;

/**
 * Handles and retrieves the login or denied page depending on the URI template
 */
@Controller
public class UserController {

    @Autowired CineastsUserDetailsService userDetailsService;
    @Autowired CineastsRepository repository;

    private final static Logger logger = LoggerFactory.getLogger(UserController.class);

    @RequestMapping(value = "/user", method = RequestMethod.GET)
    public String profile(Model model) {
        final User user = userDetailsService.getUserFromSession();
        model.addAttribute("user", user);
        model.addAttribute("recommendations", repository.recommendMovies(user, 3));
        return "/user/index";
    }

    @RequestMapping(value = "/user/{login}/friends", method = RequestMethod.POST)
    public String addFriend(Model model, @PathVariable("login") String login) {
        userDetailsService.addFriend(login);
		return "forward:/user/"+login;
    }

    @RequestMapping(value = "/user/{login}")
    public String publicProfile(Model model, @PathVariable("login") String login) {
        User profiled = userDetailsService.findUser(login);
        User user = userDetailsService.getUserFromSession();

        return publicProfile(model, profiled, user);
    }

    private String publicProfile(Model model, User profiled, User user) {
        if (profiled.equals(user)) return profile(model);

        model.addAttribute("profiled", profiled);
        model.addAttribute("user", user);
        model.addAttribute("isFriend", areFriends(profiled, user));
        return "/user/public";
    }

    private boolean areFriends(User user, User loggedIn) {
        return user!=null && user.isFriend(loggedIn);
    }
}