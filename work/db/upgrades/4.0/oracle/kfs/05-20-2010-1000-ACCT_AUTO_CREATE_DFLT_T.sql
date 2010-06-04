drop table CA_ACCT_AUTO_CREATE_DFLT_T;

create table CA_ACCT_AUTO_CREATE_DFLT_T (
    KC_UNIT VARCHAR2(8), 
   	OBJ_ID VARCHAR2(36) NOT NULL,
    VER_NBR NUMBER(8,0) default 1 NOT NULL,
    FIN_COA_CD VARCHAR2(2),
	KC_UNIT_NAME VARCHAR2(40), 
	ACCT_ZIP_CD VARCHAR2(20), 
	ACCT_CITY_NM VARCHAR2(25), 
    ACCT_STATE_CD VARCHAR2(2),
    ACCT_STREET_ADDR VARCHAR2(30), 
    ACCT_OFF_CMP_IND VARCHAR2(1),
   	ACCT_TYP_CD VARCHAR2(2), 
   	ACCT_PHYS_CMP_CD VARCHAR2(2),
   	SUB_FUND_GRP_CD VARCHAR2(6), 
   	ACCT_FRNG_BNFT_CD VARCHAR2(1),
    RPTS_TO_FIN_COA_CD VARCHAR2(2), 
   	RPTS_TO_ACCT_NBR VARCHAR2(7),
 	FIN_HGH_ED_FUNC_CD VARCHAR2(4), 
    ACCT_FSC_OFC_UID VARCHAR2(40), 
    ACCT_SPVSR_UNVL_ID VARCHAR2(40), 
    ACCT_MGR_UNVL_ID VARCHAR2(40), 
    ORG_CD VARCHAR2(4), 
    CONT_FIN_COA_CD VARCHAR2(2), 
    CONT_ACCOUNT_NBR VARCHAR2(7), 
    INCOME_FIN_COA_CD VARCHAR2(2), 
    INCOME_ACCOUNT_NBR VARCHAR2(7), 
    BDGT_REC_LVL_CD VARCHAR2(1), 
    ACCT_SF_CD VARCHAR2(1), 
    ACCT_PND_SF_CD VARCHAR2(1),
    FIN_EXT_ENC_SF_CD VARCHAR2(1),
    FIN_INT_ENC_SF_CD VARCHAR2(1),
    FIN_PRE_ENC_SF_CD VARCHAR2(1),
    FIN_OBJ_PRSCTRL_CD VARCHAR2(1),
    ICR_FIN_COA_CD VARCHAR2(2), 
    ICR_ACCOUNT_NBR VARCHAR2(7), 
    CG_ACCT_RESP_ID NUMBER(2,0),
    ACCT_ICR_TYP_CD VARCHAR2(2),
    ACCT_EXPNS_GUIDE_TXT VARCHAR2(400), 
    ACCT_INCM_GUIDE_TXT VARCHAR2(400),
    ACCT_PRPS_GUIDE_TXT VARCHAR2(400),
    ACCT_DESC_CMPS_CD VARCHAR2(2),
    ACCT_DESC_BLDG_CD VARCHAR2(10),	
     ACCT_CLOSED_IND VARCHAR2(1)	
);

ALTER TABLE CA_ACCT_AUTO_CREATE_DFLT_T ADD CONSTRAINT CA_ACCT_AUTO_CREATE_DFLT_PK PRIMARY KEY (KC_UNIT);

