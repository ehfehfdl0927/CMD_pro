package com.CMD.CMD_pro.survey.mapper;

import com.CMD.CMD_pro.survey.domain.*;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SurveyMapper {
    public List<SurveyVO> selectSurveyList(SearchCriteria cri) throws Exception;
    //    public PageMaker selectCountPaging(SearchCriteria cri) throws Exception;
    public int selectCountPaging() throws Exception;
    public SurveyVO selectSurvey(int survey_index);
    public List<SurveyItemVO> selectSurveyItems(int survey_index);
    public List<Integer> selectSurveyResult(int survey_index);
    public List<ResultDataSet> selectSurveyResultDataSet(int survey_index);
    public int resultCount(int survey_item_index);
    public void insertSurvey(SurveyVO surveyVO);
    public void insertSurveyItem(List<SurveyItemVO> itemlist);
    public void insertSurveyResult(SurveyResultVO surveyResultVO);
    public void closeSurvey(int survey_index);
    public void removeSurvey(int survey_index);
    public SurveyItemVO selectMySurveyResult(int survey_index, int user_index);

    public int existsVote(@Param("surveyIndex") int surveyIndex, @Param("userIndex") int userIndex);
//    public List<SurveyVO> selectSearchSurvey(SearchCriteria cri);
//    public void searchInsert(@Param("keyword") String keyword) throws Exception;
//    public List<SurveyVO> mainSearch(@Param("keyword") String keyword) throws Exception;
//    public List<SurveyVO> mainSearch(@Param("keyword") String keyword) throws Exception;
//    public int mainSearchCount(@Param("keyword") String keyword) throws Exception;
}
