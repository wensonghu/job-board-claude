--
-- PostgreSQL database dump
--

\restrict y1azx4lssvlwRhGmIbizTkBgu6joMVSb5h30a9MXKnsjh0veo1tEp925QOhLXHM

-- Dumped from database version 16.12 (Homebrew)
-- Dumped by pg_dump version 16.12 (Homebrew)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Data for Name: app_user; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.app_user (id, auth_provider, created_at, display_name, email, email_verified, google_sub, password_hash, user_key, verification_token, verification_token_expiry, role, user_type, last_seen_broadcast_id) FROM stdin;
2	LOCAL	2026-02-24 11:44:10.916477	jun	evelynhe1022@gmail.com	t	\N	$2a$10$Y6s.FKXjrpC15IK7r5ez0uQJYP3/5Gi9OIPT/hr27NuAoNFWiwiS.	0a8569a3e93d97cdf55caa8eb5e75987134ea4ff75272c61b4de633ff06b972d	\N	\N	BASIC	BETA	\N
1	GOOGLE	2026-02-23 16:00:36.470643	Wensong	wensonghu@gmail.com	t	110554898588109759279	\N	88d9344636e218b38470df5c98da553ee5dc57d08ff2d23f4e6d0d16e8a6fcab	\N	\N	ADMIN	INTERNAL	5
\.


--
-- Data for Name: broadcast_message; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.broadcast_message (id, active, content, created_at, created_by) FROM stdin;
1	f	WELCOME TO PITSTOP, GLUCK ON YOUR SEARCH!	2026-02-24 23:19:31.908836-08	1
2	f	hello	2026-02-24 23:29:47.002744-08	1
3	f	Welcome, Good luck with your job search!	2026-02-24 23:52:22.615987-08	1
4	f	Welcome to Pitstop - your personal assistant as you are finding the next incredible positionalalalal	2026-02-25 10:01:54.894855-08	1
5	t	Welcome!	2026-02-25 10:57:22.453746-08	1
\.


--
-- Data for Name: card; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.card (id, company, date, details, interview_date, "position", referred_by, stage, status, user_id) FROM stdin;
2	Transcarent	2026-02-17	Greg Hnatiuk - VP of Eng	2026-02-18|09:30|PT	Director of Product	Internal Recruiter Kris Minkel	NEXT_ROUNDS	INTERVIEW_COMPLETED	1
3	Transcarent	2026-02-17	Brad Wolter, Director of Product	2026-02-19|11:00|PT	Director of Product	\N	NEXT_ROUNDS	INTERVIEW_COMPLETED	1
4	Transcarent	2026-02-17	Arielle Kahn - SPM Platform	2026-02-19|14:00|PT	Director of Product	\N	NEXT_ROUNDS	INTERVIEW_COMPLETED	1
5	Transcarent	2026-02-17	Hiren Bhavsar, VP, Product Enterprise Growth	2026-02-20|12:00|PT	Director of Product	\N	NEXT_ROUNDS	INTERVIEW_COMPLETED	1
6	Hippocratic AI	2026-02-13	Next round pending	TBD	Product Lead, Payor	Natalie Marrota	HM	INTERVIEW_COMPLETED	1
7	Hippocratic AI	2026-02-20	Onsite round	2026-03-03	Product Lead	\N	FINAL	INTERVIEW_DATE_CONFIRMED	1
8	Datavant	2026-02-12	\N	2026-02-19|08:30|PT	Senior Director, Provider Experience	\N	HM	REJECTED	1
9	Decagon AI	2026-02-02	\N	\N	Connected with Lukas on Product	\N	SEEDING	REJECTED	1
10	Headspace	2026-01-30	\N	TBD	Director of Product	Preetham	SEEDING	IN_PROGRESS	1
11	Anthropic	2026-02-03	Reached out to Ivy W, Rebecca M, Catherine Wu, Yolanda	\N	Product Lead	pending	SEEDING	IN_PROGRESS	1
12	Tempus AI	2026-02-13	\N	TBD	Director Product	Eric Goldner	SEEDING	IN_PROGRESS	1
13	Lyra	2026-02-13	\N	TBD	Director of Product	Chris Toomey and his colleague	SEEDING	IN_PROGRESS	1
15	Spring Health	2026-01-31	\N	\N	Associate Director Product	Bobby K. (pending)	SEEDING	IN_PROGRESS	1
16	Oak FT (VC Portfolio)	2026-02-20	\N	2026-02-26|09:00	TBD	Will Granchi	RECRUITER	INTERVIEW_DATE_CONFIRMED	1
14	Wondr Health - GM	2026-02-13		2026-02-23|09:00|PT	General Manager	Will Granchi (recruiter)	HM	REJECTED	1
1	Transcarent	2026-02-20		2026-02-27|13:00	Director of Product, Platform	Internal Recruiter Kris Minkel	FINAL	INTERVIEW_DATE_CONFIRMED	1
\.


--
-- Data for Name: card_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.card_history (id, card_id, changed_at, company, date, details, interview_date, is_deleted, "position", referred_by, sheet_id, stage, status, user_id) FROM stdin;
1	14	2026-02-24 10:19:14.815656-08	Wondr Health - GM	2026-02-13		2026-02-23|09:00|PT	f	General Manager	Will Granchi (recruiter)	\N	HM	REJECTED	1
2	\N	2026-02-13 17:09:10.717-08	Transcarent	2026-02-12	\N	TBD	f	Director of Product, Platform	Internal Recruiter Kris Minkel	1770355148497	NEXT_ROUNDS	INTERVIEW_SCHEDULE_PENDING	1
3	\N	2026-02-13 17:09:10.846-08	Hippocratic AI	2026-02-13	Interview with Vishal 2/14 Confirmed	2026-02-14	f	Product Lead, Payor	Natalie Marrota	1770397632443	HM	INTERVIEW_DATE_CONFIRMED	1
4	\N	2026-02-13 17:09:11.189-08	Datavant	2026-02-12	\N	TBD	f	Senior Director, Provider Experience	\N	1770397703575	HM	INTERVIEW_SCHEDULE_PENDING	1
5	\N	2026-02-13 17:09:11.312-08	Decagon AI	2026-02-02	\N	\N	f	Connected with Lukas on Product	\N	1770397846392	SEEDING	REJECTED	1
6	\N	2026-02-13 17:09:11.45-08	Headspace	2026-01-30	\N	TBD	f	Director of Product	Preetham	1770397891875	SEEDING	IN_PROGRESS	1
7	\N	2026-02-13 17:09:11.583-08	Anthropic	2026-02-03	Reached out to Ivy W, Rebecca M, Catherine Wu, Yolanda	\N	f	Product Lead	pending	1770397933142	SEEDING	IN_PROGRESS	1
8	\N	2026-02-13 17:09:11.711-08	Tempus AI	2026-02-13	\N	TBD	f	Director Product	Eric Goldner	1771005633073	SEEDING	IN_PROGRESS	1
9	\N	2026-02-13 17:09:11.916-08	Lyra	2026-02-13	\N	TBD	f	Director of Product	Chris Toomey and his colleague	1771005673590	SEEDING	IN_PROGRESS	1
10	\N	2026-02-13 17:09:12.043-08	Wondr Health - GM	2026-02-13	\N	2026-02-23	f	General Manager	Will Granchi (recruiter)	1771005742346	HM	INTERVIEW_DATE_CONFIRMED	1
11	\N	2026-02-13 17:09:12.181-08	Spring Health	2026-01-31	\N	\N	f	Associate Director Product	Bobby K. (pending)	1771005871087	SEEDING	IN_PROGRESS	1
12	\N	2026-02-13 17:09:33.6-08	Transcarent	2026-02-12	\N	TBD	f	Director of Product, Platform	Internal Recruiter Kris Minkel	1770355148497	NEXT_ROUNDS	INTERVIEW_SCHEDULE_PENDING	1
13	\N	2026-02-13 17:09:33.721-08	Hippocratic AI	2026-02-13	Interview with Vishal 2/14 Confirmed	2026-02-14	f	Product Lead, Payor	Natalie Marrota	1770397632443	HM	INTERVIEW_DATE_CONFIRMED	1
14	\N	2026-02-13 17:09:33.851-08	Datavant	2026-02-12	\N	TBD	f	Senior Director, Provider Experience	\N	1770397703575	HM	INTERVIEW_SCHEDULE_PENDING	1
15	\N	2026-02-13 17:09:33.976-08	Decagon AI	2026-02-02	\N	\N	f	Connected with Lukas on Product	\N	1770397846392	SEEDING	REJECTED	1
16	\N	2026-02-13 17:09:34.102-08	Headspace	2026-01-30	\N	TBD	f	Director of Product	Preetham	1770397891875	SEEDING	IN_PROGRESS	1
17	\N	2026-02-13 17:09:34.222-08	Anthropic	2026-02-03	Reached out to Ivy W, Rebecca M, Catherine Wu, Yolanda	\N	f	Product Lead	pending	1770397933142	SEEDING	IN_PROGRESS	1
18	\N	2026-02-13 17:09:34.343-08	Tempus AI	2026-02-13	\N	TBD	f	Director Product	Eric Goldner	1771005633073	SEEDING	IN_PROGRESS	1
19	\N	2026-02-13 17:09:34.465-08	Lyra	2026-02-13	\N	TBD	f	Director of Product	Chris Toomey and his colleague	1771005673590	SEEDING	IN_PROGRESS	1
20	\N	2026-02-13 17:09:34.59-08	Wondr Health - GM	2026-02-13	\N	2026-02-23	f	General Manager	Will Granchi (recruiter)	1771005742346	HM	INTERVIEW_DATE_CONFIRMED	1
21	\N	2026-02-13 17:09:34.705-08	Spring Health	2026-01-31	\N	\N	f	Associate Director Product	Bobby K. (pending)	1771005871087	SEEDING	IN_PROGRESS	1
22	\N	2026-02-13 17:10:44.421-08	Transcarent	2026-02-12	\N	TBD	f	Director of Product, Platform	Internal Recruiter Kris Minkel	1770355148497	NEXT_ROUNDS	INTERVIEW_SCHEDULE_PENDING	1
23	\N	2026-02-13 17:10:44.562-08	Hippocratic AI	2026-02-13	Interview with Vishal 2/14 Confirmed	2026-02-14	f	Product Lead, Payor	Natalie Marrota	1770397632443	HM	INTERVIEW_DATE_CONFIRMED	1
24	\N	2026-02-13 17:10:44.697-08	Datavant	2026-02-12	\N	TBD	f	Senior Director, Provider Experience	\N	1770397703575	HM	INTERVIEW_SCHEDULE_PENDING	1
25	\N	2026-02-13 17:10:44.824-08	Decagon AI	2026-02-02	\N	\N	f	Connected with Lukas on Product	\N	1770397846392	SEEDING	REJECTED	1
26	\N	2026-02-13 17:10:44.949-08	Headspace	2026-01-30	\N	TBD	f	Director of Product	Preetham	1770397891875	SEEDING	IN_PROGRESS	1
27	\N	2026-02-13 17:10:45.076-08	Anthropic	2026-02-03	Reached out to Ivy W, Rebecca M, Catherine Wu, Yolanda	\N	f	Product Lead	pending	1770397933142	SEEDING	IN_PROGRESS	1
28	\N	2026-02-13 17:10:45.204-08	Tempus AI	2026-02-13	\N	TBD	f	Director Product	Eric Goldner	1771005633073	SEEDING	IN_PROGRESS	1
29	\N	2026-02-13 17:10:45.336-08	Lyra	2026-02-13	\N	TBD	f	Director of Product	Chris Toomey and his colleague	1771005673590	SEEDING	IN_PROGRESS	1
30	\N	2026-02-13 17:10:45.464-08	Wondr Health - GM	2026-02-13	\N	2026-02-23	f	General Manager	Will Granchi (recruiter)	1771005742346	HM	INTERVIEW_DATE_CONFIRMED	1
31	\N	2026-02-13 17:10:45.59-08	Spring Health	2026-01-31	\N	\N	f	Associate Director Product	Bobby K. (pending)	1771005871087	SEEDING	IN_PROGRESS	1
32	\N	2026-02-13 17:15:59.339-08	Transcarent	2026-02-12	\N	TBD	f	Director of Product, Platform	Internal Recruiter Kris Minkel	1770355148497	NEXT_ROUNDS	INTERVIEW_SCHEDULE_PENDING	1
33	\N	2026-02-13 17:15:59.475-08	Hippocratic AI	2026-02-13	Interview with Vishal 2/14 Confirmed	2026-02-14	f	Product Lead, Payor	Natalie Marrota	1770397632443	HM	INTERVIEW_DATE_CONFIRMED	1
34	\N	2026-02-13 17:15:59.922-08	Datavant	2026-02-12	\N	TBD	f	Senior Director, Provider Experience	\N	1770397703575	HM	INTERVIEW_SCHEDULE_PENDING	1
35	\N	2026-02-13 17:16:00.06-08	Decagon AI	2026-02-02	\N	\N	f	Connected with Lukas on Product	\N	1770397846392	SEEDING	REJECTED	1
36	\N	2026-02-13 17:16:00.191-08	Headspace	2026-01-30	\N	TBD	f	Director of Product	Preetham	1770397891875	SEEDING	IN_PROGRESS	1
37	\N	2026-02-13 17:16:00.323-08	Anthropic	2026-02-03	Reached out to Ivy W, Rebecca M, Catherine Wu, Yolanda	\N	f	Product Lead	pending	1770397933142	SEEDING	IN_PROGRESS	1
38	\N	2026-02-13 17:16:00.682-08	Tempus AI	2026-02-13	\N	TBD	f	Director Product	Eric Goldner	1771005633073	SEEDING	IN_PROGRESS	1
39	\N	2026-02-13 17:16:00.818-08	Lyra	2026-02-13	\N	TBD	f	Director of Product	Chris Toomey and his colleague	1771005673590	SEEDING	IN_PROGRESS	1
40	\N	2026-02-13 17:16:00.951-08	Wondr Health - GM	2026-02-13	\N	2026-02-23	f	General Manager	Will Granchi (recruiter)	1771005742346	HM	INTERVIEW_DATE_CONFIRMED	1
41	\N	2026-02-13 17:16:01.074-08	Spring Health	2026-01-31	\N	\N	f	Associate Director Product	Bobby K. (pending)	1771005871087	SEEDING	IN_PROGRESS	1
42	\N	2026-02-13 17:16:46.625-08	Transcarent	2026-02-12	\N	TBD	f	Director of Product, Platform	Internal Recruiter Kris Minkel	1770355148497	NEXT_ROUNDS	INTERVIEW_SCHEDULE_PENDING	1
43	\N	2026-02-13 17:16:46.753-08	Hippocratic AI	2026-02-13	Interview with Vishal 2/14 Confirmed	2026-02-14	f	Product Lead, Payor	Natalie Marrota	1770397632443	HM	INTERVIEW_DATE_CONFIRMED	1
44	\N	2026-02-13 17:16:46.886-08	Datavant	2026-02-12	\N	TBD	f	Senior Director, Provider Experience	\N	1770397703575	HM	INTERVIEW_SCHEDULE_PENDING	1
45	\N	2026-02-13 17:16:47.02-08	Decagon AI	2026-02-02	\N	\N	f	Connected with Lukas on Product	\N	1770397846392	SEEDING	REJECTED	1
46	\N	2026-02-13 17:16:47.153-08	Headspace	2026-01-30	\N	TBD	f	Director of Product	Preetham	1770397891875	SEEDING	IN_PROGRESS	1
47	\N	2026-02-13 17:16:47.278-08	Anthropic	2026-02-03	Reached out to Ivy W, Rebecca M, Catherine Wu, Yolanda	\N	f	Product Lead	pending	1770397933142	SEEDING	IN_PROGRESS	1
48	\N	2026-02-13 17:16:47.413-08	Tempus AI	2026-02-13	\N	TBD	f	Director Product	Eric Goldner	1771005633073	SEEDING	IN_PROGRESS	1
49	\N	2026-02-13 17:16:47.548-08	Lyra	2026-02-13	\N	TBD	f	Director of Product	Chris Toomey and his colleague	1771005673590	SEEDING	IN_PROGRESS	1
50	\N	2026-02-13 17:16:47.68-08	Wondr Health - GM	2026-02-13	\N	2026-02-23	f	General Manager	Will Granchi (recruiter)	1771005742346	HM	INTERVIEW_DATE_CONFIRMED	1
51	\N	2026-02-13 17:16:47.817-08	Spring Health	2026-01-31	\N	\N	f	Associate Director Product	Bobby K. (pending)	1771005871087	SEEDING	IN_PROGRESS	1
52	\N	2026-02-13 17:17:30.622-08	test	2026-02-14	adaf	\N	f	test1	adfa	1771031850622	SEEDING	IN_PROGRESS	1
53	\N	2026-02-13 21:14:27.327-08	test	2026-02-14	adaf	\N	t	test1	adfa	1771031850622	SEEDING	IN_PROGRESS	1
54	\N	2026-02-13 21:14:41.756-08	Datavant	2026-02-12	\N	2026-02-19	f	Senior Director, Provider Experience	\N	1770397703575	HM	INTERVIEW_DATE_CONFIRMED	1
55	\N	2026-02-14 12:37:42.336-08	Datavant	2026-02-12	\N	2026-02-19|08:30|PT	f	Senior Director, Provider Experience	\N	1770397703575	HM	INTERVIEW_DATE_CONFIRMED	1
56	\N	2026-02-14 12:39:08.439-08	Wondr Health - GM	2026-02-13	\N	2026-02-23|09:00|PT	f	General Manager	Will Granchi (recruiter)	1771005742346	HM	INTERVIEW_DATE_CONFIRMED	1
57	\N	2026-02-14 12:39:20.466-08	Hippocratic AI	2026-02-13	Interview with Vishal 2/14 Confirmed	2026-02-14	f	Product Lead, Payor	Natalie Marrota	1770397632443	NEXT_ROUNDS	INTERVIEW_DATE_CONFIRMED	1
58	\N	2026-02-14 12:39:35.677-08	Hippocratic AI	2026-02-13	Interview with Vishal 2/14 Confirmed	TBD	f	Product Lead, Payor	Natalie Marrota	1770397632443	NEXT_ROUNDS	IN_PROGRESS	1
59	\N	2026-02-14 12:39:45.738-08	Hippocratic AI	2026-02-13	Interview with Vishal 2/14 Confirmed	TBD	f	Product Lead, Payor	Natalie Marrota	1770397632443	HM	IN_PROGRESS	1
60	\N	2026-02-14 12:40:04.35-08	Hippocratic AI	2026-02-13	next round pending	TBD	f	Product Lead, Payor	Natalie Marrota	1770397632443	HM	IN_PROGRESS	1
61	\N	2026-02-14 12:40:12.073-08	Hippocratic AI	2026-02-13	Next round pending	TBD	f	Product Lead, Payor	Natalie Marrota	1770397632443	HM	IN_PROGRESS	1
62	\N	2026-02-14 20:31:31.872-08	Hippocratic AI	2026-02-13	Next round pending	2026-02-15	f	Product Lead, Payor	Natalie Marrota	1770397632443	HM	INTERVIEW_DATE_CONFIRMED	1
63	\N	2026-02-14 20:32:05.133-08	test1	2026-02-15	\N	2026-02-15	f	test1	\N	1771129925132	HM	INTERVIEW_DATE_CONFIRMED	1
64	\N	2026-02-14 20:32:06.667-08	test1	2026-02-15	\N	2026-02-15	f	test1	\N	1771129926667	HM	INTERVIEW_DATE_CONFIRMED	1
65	\N	2026-02-14 20:32:26.683-08	Hippocratic AI	2026-02-13	Next round pending	TBD	f	Product Lead, Payor	Natalie Marrota	1770397632443	HM	IN_PROGRESS	1
66	\N	2026-02-14 20:36:34.87-08	test2	2026-02-15	\N	2026-02-16	f	test2	\N	1771130194869	RECRUITER	INTERVIEW_DATE_CONFIRMED	1
67	\N	2026-02-14 20:37:03.961-08	test2	2026-02-15	\N	2026-02-16	f	test2	\N	1771130194869	HM	INTERVIEW_DATE_CONFIRMED	1
68	\N	2026-02-14 20:37:29.463-08	test1	2026-02-15	\N	2026-02-15	t	test1	\N	1771129926667	HM	INTERVIEW_DATE_CONFIRMED	1
69	\N	2026-02-14 20:46:42.35-08	test2	2026-02-15	\N	2026-02-15	f	test2	\N	1771130194869	HM	INTERVIEW_DATE_CONFIRMED	1
70	\N	2026-02-14 20:54:07.982-08	test1	2026-02-15	\N	2026-02-15	t	test1	\N	1771129925132	HM	INTERVIEW_DATE_CONFIRMED	1
71	\N	2026-02-14 20:54:10.167-08	test2	2026-02-15	\N	2026-02-15	t	test2	\N	1771130194869	HM	INTERVIEW_DATE_CONFIRMED	1
72	\N	2026-02-15 22:30:15.138-08	test	2026-02-16	\N	2026-02-15	f	Test	\N	1771223415137	SEEDING	INTERVIEW_DATE_CONFIRMED	1
73	\N	2026-02-17 13:58:34.652-08	Transcarent	2026-02-17	Greg Hnatiuk - VP of Eng	2026-02-18|09:30|PT	f	Director of Product, Platform	Internal Recruiter Kris Minkel	1770355148497	NEXT_ROUNDS	INTERVIEW_DATE_CONFIRMED	1
74	\N	2026-02-17 14:00:36.2-08	Transcarent	2026-02-17	Brad Wolter, Director of Product	2026-02-19|11:00|PT	f	Director of Product	\N	1771365636200	NEXT_ROUNDS	INTERVIEW_DATE_CONFIRMED	1
75	\N	2026-02-17 14:02:08.016-08	Transcarent	2026-02-17	Arielle Kahn - SPM Platform	2026-02-19|14:00|PT	f	Director of Product	\N	1771365728016	NEXT_ROUNDS	INTERVIEW_DATE_CONFIRMED	1
76	\N	2026-02-17 14:03:35.315-08	Transcarent	2026-02-17	Hiren Bhavsar, VP, Product Enterprise Growth	2026-02-20|12:00|PT	f	Director of Product	\N	1771365815315	NEXT_ROUNDS	INTERVIEW_DATE_CONFIRMED	1
77	\N	2026-02-17 14:04:48.363-08	Transcarent	2026-02-17	Chris Stevens, Clinical Programs Sr. Dir	2026-02-23|11:00|PT	f	Director of Product	\N	1771365888363	NEXT_ROUNDS	INTERVIEW_DATE_CONFIRMED	1
78	\N	2026-02-18 11:26:49.337-08	Transcarent	2026-02-17	Chris Stevens, Clinical Programs Sr. Dir	2026-02-23|11:00|PT	t	Director of Product	\N	1771365888363	NEXT_ROUNDS	INTERVIEW_DATE_CONFIRMED	1
79	\N	2026-02-18 14:22:49.088-08	test	2026-02-16	\N	2026-02-15	t	Test	\N	1771223415137	SEEDING	INTERVIEW_DATE_CONFIRMED	1
80	\N	2026-02-20 11:34:14.61-08	Hippocratic AI	2026-02-20	Onsite round	2026-03-03	f	Product Lead	\N	1771616054609	FINAL	INTERVIEW_DATE_CONFIRMED	1
81	\N	2026-02-20 11:38:15.373-08	Oak FT (VC Portfolio)	2026-02-20	\N	\N	f	TBD	Will Granchi	1771616295372	SEEDING	IN_PROGRESS	1
82	\N	2026-02-20 11:38:29.279-08	Oak FT (VC Portfolio)	2026-02-20	Scheduling with Mason	\N	f	TBD	Will Granchi	1771616295372	SEEDING	IN_PROGRESS	1
83	\N	2026-02-20 11:49:21.962-08	Datavant	2026-02-12	\N	2026-02-19|08:30|PT	f	Senior Director, Provider Experience	\N	1770397703575	HM	INTERVIEW_DATE_CONFIRMED	1
84	\N	2026-02-20 11:49:39.309-08	Datavant	2026-02-12	\N	2026-02-19|08:30|PT	f	Senior Director, Provider Experience	\N	1770397703575	HM	INTERVIEW_DATE_CONFIRMED	1
85	\N	2026-02-20 13:15:03.603-08	Transcarent	2026-02-17	Greg Hnatiuk - VP of Eng	2026-02-18|09:30|PT	f	Director of Product, Platform	Internal Recruiter Kris Minkel	1770355148497	NEXT_ROUNDS	INTERVIEW_COMPLETED	1
86	\N	2026-02-20 13:15:10.71-08	Transcarent	2026-02-17	Brad Wolter, Director of Product	2026-02-19|11:00|PT	f	Director of Product	\N	1771365636200	NEXT_ROUNDS	INTERVIEW_COMPLETED	1
87	\N	2026-02-20 13:15:44.811-08	Transcarent	2026-02-17	Arielle Kahn - SPM Platform	2026-02-19|14:00|PT	f	Director of Product	\N	1771365728016	NEXT_ROUNDS	INTERVIEW_COMPLETED	1
88	\N	2026-02-20 13:15:49.91-08	Transcarent	2026-02-17	Hiren Bhavsar, VP, Product Enterprise Growth	2026-02-20|12:00|PT	f	Director of Product	\N	1771365815315	NEXT_ROUNDS	INTERVIEW_COMPLETED	1
89	\N	2026-02-20 13:16:00.188-08	Datavant	2026-02-12	\N	2026-02-19|08:30|PT	f	Senior Director, Provider Experience	\N	1770397703575	HM	INTERVIEW_COMPLETED	1
90	\N	2026-02-20 13:16:19.204-08	Hippocratic AI	2026-02-13	Next round pending	TBD	f	Product Lead, Payor	Natalie Marrota	1770397632443	HM	INTERVIEW_COMPLETED	1
91	\N	2026-02-20 14:34:51.486-08	Oak FT (VC Portfolio)	2026-02-20	Scheduling with Mason	\N	f	TBD	Will Granchi	1771616295372	RECRUITER	IN_PROGRESS	1
92	\N	2026-02-20 14:35:24.338-08	Oak FT (VC Portfolio)	2026-02-20	\N	2026-02-26|09:00	f	TBD	Will Granchi	1771616295372	RECRUITER	INTERVIEW_DATE_CONFIRMED	1
93	\N	2026-02-20 14:35:50.31-08	Transcarent	2026-02-20	\N	TBD	f	Director of Product, Platform	Internal Recruiter Kris Minkel	1771626950309	FINAL	INTERVIEW_SCHEDULE_PENDING	1
94	\N	2026-02-21 10:02:16.977-08	Datavant	2026-02-12	\N	2026-02-19|08:30|PT	f	Senior Director, Provider Experience	\N	1770397703575	HM	REJECTED	1
95	\N	2026-02-23 10:28:39.207-08	Wondr Health - GM	2026-02-13	\N	2026-02-23|09:00|PT	f	General Manager	Will Granchi (recruiter)	1771005742346	HM	INTERVIEW_COMPLETED	1
96	21	2026-02-24 10:43:10.26472-08	test	2026-02-24		TBD	f	test1		\N	SEEDING	IN_PROGRESS	1
97	21	2026-02-24 10:44:20.049175-08	test	2026-02-24		TBD	f	test1		\N	SEEDING	INTERVIEW_SCHEDULE_PENDING	1
98	21	2026-02-24 10:44:47.930689-08	test	2026-02-24		TBD	t	test1		\N	SEEDING	INTERVIEW_SCHEDULE_PENDING	1
99	22	2026-02-24 11:49:52.760327-08	testeve	2026-02-24		TBD	f	test2		\N	SEEDING	IN_PROGRESS	2
100	1	2026-02-24 12:10:34.151063-08	Transcarent	2026-02-20		2026-02-27|13:00	f	Director of Product, Platform	Internal Recruiter Kris Minkel	\N	FINAL	INTERVIEW_DATE_CONFIRMED	1
101	23	2026-02-24 12:12:02.350997-08	test	2026-02-24		TBD	f	test1		\N	SEEDING	IN_PROGRESS	1
102	23	2026-02-24 12:12:14.22221-08	test	2026-02-24		TBD	f	test1		\N	SEEDING	INTERVIEW_SCHEDULE_PENDING	1
103	24	2026-02-24 16:08:53.896015-08	test	2026-02-25		TBD	f	test2		\N	SEEDING	IN_PROGRESS	1
104	24	2026-02-24 16:09:12.250057-08	test	2026-02-25		TBD	f	test2		\N	RECRUITER	IN_PROGRESS	1
105	24	2026-02-24 16:09:53.944826-08	test	2026-02-25		2026-02-25|16:09|PT	f	test2		\N	RECRUITER	INTERVIEW_DATE_CONFIRMED	1
106	23	2026-02-24 16:21:18.815797-08	test	2026-02-24		TBD	t	test1		\N	SEEDING	INTERVIEW_SCHEDULE_PENDING	1
107	24	2026-02-24 16:21:24.084677-08	test	2026-02-25		2026-02-25|16:09|PT	t	test2		\N	RECRUITER	INTERVIEW_DATE_CONFIRMED	1
108	22	2026-02-25 10:58:40.092779-08	testeve	2026-02-24		TBD	t	test2		\N	SEEDING	IN_PROGRESS	2
\.


--
-- Name: app_user_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.app_user_id_seq', 2, true);


--
-- Name: broadcast_message_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.broadcast_message_id_seq', 5, true);


--
-- Name: card_history_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.card_history_id_seq', 108, true);


--
-- Name: card_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.card_id_seq', 24, true);


--
-- PostgreSQL database dump complete
--

\unrestrict y1azx4lssvlwRhGmIbizTkBgu6joMVSb5h30a9MXKnsjh0veo1tEp925QOhLXHM

