from pydantic_settings import BaseSettings


class A(BaseSettings):
    b: str


A().<caret>

