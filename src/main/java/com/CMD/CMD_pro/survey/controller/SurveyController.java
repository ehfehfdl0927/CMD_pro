package com.CMD.CMD_pro.survey.controller;

import com.CMD.CMD_pro.survey.domain.*;
import com.CMD.CMD_pro.survey.mapper.SurveyMapper;
import com.CMD.CMD_pro.user.domain.UserVO;
import com.CMD.CMD_pro.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.*;

@Controller
//@RequestMapping("/survey/*") //현재 믈래스의 모든 메서드들의 기본적인 URL경로
public class SurveyController {


    private final UserMapper userMapper;
    private final SurveyMapper surveyMapper;

    @Autowired
    public SurveyController(UserMapper userMapper, SurveyMapper surveyMapper) {
        this.userMapper = userMapper;
        this.surveyMapper = surveyMapper;
    }

    @RequestMapping(value = "/mainSurvey", method = RequestMethod.GET) //설문조사 메인화면
    public String mainSurvey(@ModelAttribute("cri") SearchCriteria cri, Model model, HttpSession session) throws Exception {
        String filename = null;
        int manager = 0;
        UserVO user = new UserVO();

        if(session.getAttribute("id") != null){
            String userID = (String) session.getAttribute("id");
            user = userMapper.userLogin(userID);
            filename = user.getUser_profile();

        } else {
            filename = "non";
        }
        model.addAttribute("filename",filename);

        List<SurveyVO> surveyList = surveyMapper.selectSurveyList(cri);

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < surveyList.size(); i++){
            Date survey_reg = surveyList.get(i).getSurvey_reg();
            String real_date = format.format(survey_reg);
            surveyList.get(i).setReal_date(real_date);
        }

        model.addAttribute("surveyList", surveyList);//survey 리스트 출력

        PageMaker pageMaker = new PageMaker();
        pageMaker.setCri(cri);
        pageMaker.setTotalCount(surveyMapper.selectCountPaging());

        model.addAttribute("pageMaker", pageMaker); // 게시판 하단의 페이징 관련, 이전페이지, 페이지 링크 , 다음 페이지
        if(session.getAttribute("id") != null){
            manager = user.getUser_manager();
            model.addAttribute("manager",manager);
        } else {
            model.addAttribute("manager",manager);
        }
        return "mainSurvey";
    }

    @RequestMapping(value = "/readSurvey") //진행중인 설문조사 읽기
    public String readSurvey(@RequestParam("survey_index") int survey_index, @RequestParam("progressing") int progressing,
                             @ModelAttribute("cri") SearchCriteria cri, Model model, HttpSession session) throws Exception{
        String filename;
        String userID;
        if(session.getAttribute("id") == null){
            model.addAttribute("msg","로그인이 되어있지 않습니다.");
            model.addAttribute("url","main");
            return "alert";
        }
        if((String) session.getAttribute("id") != null){
            userID = (String) session.getAttribute("id");
            UserVO user = userMapper.userLogin(userID);
            filename = user.getUser_profile();

        } else {
            filename = "non";
        }

        boolean isProgressing = progressing == 1 ? true : false;
        List<SurveyItemVO> surveyItemList = null;
        if(isProgressing) { //설문조사가 실행중일때
            SurveyVO surveyVO = surveyMapper.selectSurvey(survey_index);
            surveyItemList = surveyMapper.selectSurveyItems(survey_index);

            model.addAttribute("surveyVO", surveyVO); // 타이틀, 내용만 페이지에 보여지게 html 작성
            model.addAttribute("surveyItemList", surveyItemList);//진행중인 설문조사 상세 페이지
            model.addAttribute("filename",filename);
            return "readSurvey_on";
        }
        else{ //설문조사 마감일때
            SurveyVO surveyVO = surveyMapper.selectSurvey(survey_index);
            surveyItemList = surveyMapper.selectSurveyItems(survey_index);
            List<ResultDataSet> dataset  = surveyMapper.selectSurveyResultDataSet(survey_index);
            List<Integer> resultIndexList = surveyMapper.selectSurveyResult(survey_index);
            List<Integer> countList = new ArrayList<>();

            for (int i = 0; i < resultIndexList.size(); i++){
                int surveyItemIndex = resultIndexList.get(i);
                int resultCount = surveyMapper.resultCount(surveyItemIndex);
                countList.add(resultCount);
            }

            model.addAttribute("surveyVO", surveyVO); // 타이틀, 내용만 페이지에 보여지게 html 작성
            model.addAttribute("surveyItemList", surveyItemList); //설문조사 선택 리스트보기
            model.addAttribute("dataset", dataset);
            model.addAttribute("countList", countList);
            model.addAttribute("filename",filename);



            int number = 0;
            for(int i=0; i<countList.size(); i++){
                number += countList.get(i);
            }
            List<Double> doubleList = new ArrayList<>();
            for(int i=0; i<countList.size(); i++){
                //testList.set(i, testList.get(i) / number * 100);

                double a = countList.get(i) / (double)number * 100.0;
                a = Math.round(a);
                doubleList.add(a);
            }

            model.addAttribute("testList",doubleList);
            return "readSurvey_off";
        }
    }

    @RequestMapping("/closeSurvey") //설문조사 마감처리
    public String closeSurvey(@RequestParam("survey_index") int survey_index, HttpSession session, Model model) throws Exception{
        String userID = (String)session.getAttribute("id");
        if(userID == null){ //로그인 확인
            model.addAttribute("msg","로그인이 되어있지 않습니다.");
            model.addAttribute("url","login");
            return "alert";
        }
        UserVO user = userMapper.userLogin(userID);
        if(user.getUser_manager() == 0){ //매니저만 설문 마감 가능
            model.addAttribute("msg","접근할 수 없습니다.");
            model.addAttribute("url","mainSurvey");
            return "alert";
        }
        try {
            surveyMapper.closeSurvey(survey_index);
        } catch (Exception e) {
            return "redirect:/mainSurvey"; //닫는거 실패했을때
        }
        return "redirect:/mainSurvey"; //설문조사 마감 성공
    }

    @RequestMapping(value="/insertSurvey",method = RequestMethod.GET)
    public String addSurveyGET(Model model, HttpSession session) throws Exception {
        String filename = null;
        if (session.getAttribute("id") != null) {
            String userID = (String) session.getAttribute("id");
            UserVO user = userMapper.userLogin(userID);
            filename = user.getUser_profile();

        } else {
            filename = "non";
        }
        model.addAttribute("filename", filename);
        String userID = (String)session.getAttribute("id");
        if(userID == null){ //로그인 확인
            model.addAttribute("msg","로그인이 되어있지 않습니다.");
            model.addAttribute("url","login");
            return "alert";
        }
        UserVO user = userMapper.userLogin(userID);
        if(user.getUser_manager() == 0){ //매니저만 추가가능
            model.addAttribute("msg","접근할 수 없는 권한입니다.");
            model.addAttribute("url","mainSurvey");
            return "alert";
        }
        return "insertSurvey";
    }

    @RequestMapping(value="insertSurvey", method = RequestMethod.POST) //설문조사 추가하기
    public String addSurvey(@RequestParam("survey_title") String survey_title, @RequestParam("survey_content") String survey_content,
                            @RequestParam("itemcontent") String [] itemcontent, @RequestParam("survey_end") String survey_end,
                            Model model, HttpSession session) throws Exception {
        String filename = null;

        if(session.getAttribute("id") != null){
            String userID = (String) session.getAttribute("id");
            UserVO user = userMapper.userLogin(userID);
            filename = user.getUser_profile();

        } else {
            filename = "non";
        }
        model.addAttribute("filename",filename);

        SurveyVO surveyVO = new SurveyVO();
        String pattern = "YYYY-MM-DD";
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        surveyVO.setSurvey_end(sdf.parse(survey_end));
        surveyVO.setSurvey_title(survey_title);
        surveyVO.setSurvey_content(survey_content);
        List<SurveyItemVO> surveyItemList = new ArrayList<>();

        surveyMapper.insertSurvey(surveyVO);
        for (int i = 0; i < itemcontent.length; i++) {
            SurveyItemVO temp  = new SurveyItemVO();
            temp.setSurvey_item_content(itemcontent[i]);
            temp.setSurvey_index(surveyVO.getSurvey_index());
            surveyItemList.add(temp);
        }
        surveyMapper.insertSurveyItem(surveyItemList);

        return "redirect:/mainSurvey";
    }


    @ResponseBody
    @RequestMapping(value = "/voteSurvey", method = RequestMethod.POST)
    public int insertSurveyResult(HttpServletRequest req,HttpSession session) throws Exception{
        String userID;
        userID = (String) session.getAttribute("id");
        UserVO user = userMapper.userLogin(userID);
        int userIndex = user.getUser_index();
        String index = req.getParameter("index");
        int surveyIndex = Integer.parseInt(index);

        if(req.getParameter("survey") == null){
            return -1;
        }
        String survey = req.getParameter("survey");

        int result = 0;

        int exists = surveyMapper.existsVote(surveyIndex,userIndex);
        if(exists == 1){
         return result;
        } else {

        SurveyResultVO surveyResultVO = new SurveyResultVO();
        surveyResultVO.setSurvey_item_index(Integer.parseInt(survey));
        surveyResultVO.setUser_index(userIndex);
        surveyResultVO.setSurvey_index(surveyIndex);
        surveyMapper.insertSurveyResult(surveyResultVO);
        result = 1;
        return result;
        }

    }

    @RequestMapping("/removeSurvey") //설문조사 지우기
    public String removeSurvey(@RequestParam("survey_index") int survey_index,
                               Model model, HttpSession session) throws Exception{
        String userID = (String)session.getAttribute("id");
        if(userID == null){ //로그인 확인
            model.addAttribute("msg","로그인이 되어있지 않습니다.");
            model.addAttribute("url","login");
            return "alert";
        }
        UserVO user = userMapper.userLogin(userID);
        if(user.getUser_manager() == 0){ //매니저만 설문 삭제 가능
            model.addAttribute("msg","접근할 수 없습니다.");
            model.addAttribute("url","mainSurvey");
            return "alert";
        }
        try {
            surveyMapper.removeSurvey(survey_index);
        } catch (Exception e) {
            return "redirect:/mainSurvey";
        }
        return "redirect:/mainSurvey";
    }

    @RequestMapping("/searchSurvey") // 검색
    public String searchSurvey(Model model, @RequestParam("searchType") String searchType, @RequestParam("keyword") String keyword) throws Exception {
        SearchCriteria cri = new SearchCriteria();
        cri.setPage(1);
        cri.setPerPageNum(5);
        cri.setSearchType(searchType);
        cri.setKeyword(keyword);

//        PageMaker pageMaker = new PageMaker();
//        pageMaker.makeSearch(cri);

        List<SurveyVO> SurveyList = surveyMapper.selectSurveyList(cri);
//        surveyMapper.searchInsert(keyword);
//        model.addAttribute("searchType",searchType);
//        model.addAttribute("keyword",keyword); //키워드 전달

        model.addAttribute("surveysearch",SurveyList); //검색한 설문 리스트
        return "searchSurvey";
    }

    @GetMapping("/test")
    public String test(Model model){
        List<Integer> testList = new ArrayList<>();
        testList.add(1);
        testList.add(3);
        testList.add(5);
        int number = 0;
        for(int i=0; i<testList.size(); i++){
            number += testList.get(i);
        }
        List<Double> doubleList = new ArrayList<>();
        for(int i=0; i<testList.size(); i++){
            //testList.set(i, testList.get(i) / number * 100);

            double a = testList.get(i) / (double)number * 100.0;
            a = Math.round(a);
            System.out.println(a);
            doubleList.add(a);
        }

        System.out.println(number);
        System.out.println(doubleList);
        model.addAttribute("testList",doubleList);
        return "test";
    }

}