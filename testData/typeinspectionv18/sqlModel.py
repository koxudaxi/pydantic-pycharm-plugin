from typing import *

from sqlmodel import Field, SQLModel


class Hero(SQLModel, table=True):
    id: Optional[int] = Field(default=None, primary_key=True)
    name: str
    secret_name = Field(default="dummy", primary_key=True)
    age: Optional[int] = None

hero_1 = Hero(name="Deadpond", secret_name="Dive Wilson")
hero_2 = Hero(name="Spider-Boy", secret_name="Pedro Parqueador")
hero_3 = Hero(name="Rusty-Man", secret_name="Tommy Sharp", age=48)

hero_4 = Hero(secret_name="test"<warning descr="null">)</warning>

hero_5 = Hero(<warning descr="Expected type 'str', got 'int' instead">name=123</warning>, <warning descr="Expected type 'LiteralString', got 'int' instead">secret_name=456</warning>, <warning descr="Expected type 'Optional[int]', got 'LiteralString' instead">age="abc"</warning>)