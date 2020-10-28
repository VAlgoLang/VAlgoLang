class Stack(DataStructure, ABC):

    def __init__(self, ul, ur, ll, lr, aligned_edge, color=WHITE, text_color=WHITE, text_weight=NORMAL,font="Times New Roman"):
        super().__init__(ul, ur, ll, lr, aligned_edge, color, text_color, text_weight, font)
        self.empty = None

    def create_init(self, text):
        empty = Init_structure(text, 0, self.max_width - 2 * MED_SMALL_BUFF, color=self.color, text_color=self.text_color)
        self.empty = empty.all
        empty.all.move_to(np.array([self.width_center, self.lr[1], 0]), aligned_edge=self.aligned_edge)
        self.all.add(empty.all)
        return ShowCreation(empty.all)

    def push(self, obj, color, text_color):
        animations = []
        obj.all.move_to(np.array([self.width_center, self.ul[1], 0]), UP)
        shrink, scale_factor = self.shrink_if_cross_border(obj.all)
        if shrink:
            animations.append([shrink])
        target_width = self.all.get_width() * (scale_factor if scale_factor else 1)
        obj.all.scale(target_width / obj.all.get_width())
        animations.append([FadeIn(obj.all)])
        animations.append([ApplyMethod(obj.all.next_to, self.all, np.array([0, 0.25, 0]))])
        animations.append([FadeToColor(obj.shape, color), FadeToColor(obj.text, text_color)])
        return animations

    def pop(self, obj, color, text_color):
        self.all.remove(obj.all)
        animation = [[FadeToColor(obj.shape, color), FadeToColor(obj.text, text_color)],
                     [ApplyMethod(obj.all.move_to, np.array([self.width_center, self.ul[1], 0]), UP)],
                     [FadeOut(obj.all)]]
        enlarge, scale_factor = self.shrink(new_width=self.all.get_width(), new_height=self.all.get_height() + 0.25)
        if enlarge:
            animation[2].append(enlarge)
        return animation

    def shrink_if_cross_border(self, new_obj):
        height = new_obj.get_height()
        if self.will_cross_boundary(height, "TOP"):
            return self.shrink(new_width=self.all.get_width(), new_height=self.all.get_height() + height + 0.35)
        return 0, 1

    def poppush(self, obj, target, color, text_color):
        self.all.remove(obj.all)
        animation = [[FadeToColor(obj.shape, color), FadeToColor(obj.text, text_color)],
                     [ApplyMethod(obj.all.move_to, np.array([self.width_center, self.ul[1], 0]), UP)],
                     [ApplyMethod(obj.all.move_to, np.array([target.width_center, target.ul[1], 0]), UP)]]
        enlarge, _ = self.shrink(new_width=self.all.get_width(), new_height=self.all.get_height() + 0.25)
        if enlarge:
            animation[-1].append(enlarge)
        scale_factor = target.all.get_width() / obj.all.get_width()
        if scale_factor != 1:
            animation.append([ApplyMethod(obj.all.scale, scale_factor, {"about_edge": UP})])
        animation.append([ApplyMethod(obj.all.next_to, target.all, np.array([0, 0.25, 0]))])
        return animation


# Object representing a stack instantiation.
class Init_structure:
    def __init__(self, text, angle, length=1.5, color=WHITE, text_color=WHITE, text_weight=NORMAL, font="Times New Roman"):
        self.shape = Line(color=color)
        self.shape.set_length(length)
        self.shape.set_angle(angle)
        self.text = Text(text, color=text_color, weight=text_weight, font=font)
        self.text.next_to(self.shape, DOWN, SMALL_BUFF)
        self.all = VGroup(self.text, self.shape)
