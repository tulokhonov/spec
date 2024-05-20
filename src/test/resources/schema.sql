CREATE TABLE public.t1 (
       id int4 NOT NULL,
       name varchar NULL,
       age int4 NULL,
       score numeric NULL,
       date timestamp,
       enrolled boolean NULL,
       CONSTRAINT t1_pk PRIMARY KEY (id)
);