from pydantic import BaseModel, validator

def deco(func):
   def inner():
      return func()
   return inner

class A(BaseModel):
   a: str

   @deco
   def vali<caret>date_a(cls):
   pass
