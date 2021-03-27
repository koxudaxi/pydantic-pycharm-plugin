from pydantic import BaseModel, constr

def other_func(regex):
    pass

class Model(BaseModel):
    abc: str = other_func(regex='<caret>[^a-zA-Z]+')
